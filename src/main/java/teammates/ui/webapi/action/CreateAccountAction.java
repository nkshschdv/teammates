package teammates.ui.webapi.action;

import java.util.List;

import org.apache.http.HttpStatus;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.attributes.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.EmailSendingException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.exception.TeammatesException;
import teammates.common.exception.UnauthorizedAccessException;
import teammates.common.util.Config;
import teammates.common.util.Const;
import teammates.common.util.EmailWrapper;
import teammates.common.util.FieldValidator;
import teammates.common.util.JsonUtils;
import teammates.common.util.Logger;
import teammates.common.util.StringHelper;
import teammates.common.util.Templates;
import teammates.logic.api.EmailGenerator;
import teammates.ui.webapi.output.ApiOutput;

/**
 * Action: creates a new instructor account with sample courses.
 */
public class CreateAccountAction extends Action {

    private static final Logger log = Logger.getLogger();

    @Override
    protected AuthType getMinAuthLevel() {
        return AuthType.LOGGED_IN;
    }

    @Override
    public void checkSpecificAccessControl() {
        // Only admins can create new accounts
        if (!userInfo.isAdmin) {
            throw new UnauthorizedAccessException("Admin privilege is required to access this resource.");
        }
    }

    @Override
    public ActionResult execute() {
        String instructorName = getNonNullRequestParamValue(Const.ParamsNames.INSTRUCTOR_NAME).trim();
        String instructorEmail = getNonNullRequestParamValue(Const.ParamsNames.INSTRUCTOR_EMAIL).trim();
        String instructorInstitution = getNonNullRequestParamValue(Const.ParamsNames.INSTRUCTOR_INSTITUTION).trim();

        try {
            logic.verifyInputForAdminHomePage(instructorName, instructorInstitution, instructorEmail);
        } catch (InvalidParametersException e) {
            return new JsonResult(e.getMessage(), HttpStatus.SC_BAD_REQUEST);
        }

        String courseId = null;

        try {
            courseId = importDemoData(instructorEmail, instructorName);
        } catch (InvalidParametersException | EntityDoesNotExistException e) {
            return new JsonResult(e.getMessage(), HttpStatus.SC_BAD_REQUEST);
        }

        List<InstructorAttributes> instructorList = logic.getInstructorsForCourse(courseId);
        String joinLink = Config.getFrontEndAppUrl(Const.WebPageURIs.JOIN_PAGE)
                .withRegistrationKey(StringHelper.encrypt(instructorList.get(0).key))
                .withInstructorInstitution(instructorInstitution)
                .withParam(Const.ParamsNames.ENTITY_TYPE, Const.EntityType.INSTRUCTOR)
                .toAbsoluteString();
        EmailWrapper email = new EmailGenerator().generateNewInstructorAccountJoinEmail(
                instructorList.get(0).email, instructorName, joinLink);
        try {
            emailSender.sendEmail(email);
        } catch (EmailSendingException e) {
            log.severe("Instructor welcome email failed to send: " + TeammatesException.toStringWithStackTrace(e));
        }

        JoinLink output = new JoinLink(joinLink);
        return new JsonResult(output);
    }

    /**
     * Imports demo course for the new instructor.
     *
     * @return the ID of demo course
     */
    private String importDemoData(String instructorEmail, String instructorName)
            throws InvalidParametersException, EntityDoesNotExistException {

        String courseId = generateDemoCourseId(instructorEmail);

        String jsonString = Templates.populateTemplate(Templates.INSTRUCTOR_SAMPLE_DATA,
                // replace email
                "teammates.demo.instructor@demo.course", instructorEmail,
                // replace name
                "Demo_Instructor", instructorName,
                // replace course
                "demo.course", courseId);

        DataBundle data = JsonUtils.fromJson(jsonString, DataBundle.class);

        logic.persistDataBundle(data);

        List<FeedbackResponseCommentAttributes> frComments =
                logic.getFeedbackResponseCommentForGiver(courseId, instructorEmail);
        List<StudentAttributes> students = logic.getStudentsForCourse(courseId);
        List<InstructorAttributes> instructors = logic.getInstructorsForCourse(courseId);

        logic.putFeedbackResponseCommentDocuments(frComments);
        logic.putStudentDocuments(students);
        logic.putInstructorDocuments(instructors);

        return courseId;
    }

    // Strategy to Generate New Demo Course Id:
    // a. keep the part of email before "@"
    //    replace "@" with "."
    //    replace email host with their first 3 chars. eg, gmail.com -> gma
    //    append "-demo"
    //    to sum up: lebron@gmail.com -> lebron.gma-demo
    //
    // b. if the generated courseId already exists, create another one by appending a integer to the previous courseId.
    //    if the newly generate id still exists, increment the id, until we find a feasible one
    //    eg.
    //    lebron@gmail.com -> lebron.gma-demo  // already exists!
    //    lebron@gmail.com -> lebron.gma-demo0 // already exists!
    //    lebron@gmail.com -> lebron.gma-demo1 // already exists!
    //    ...
    //    lebron@gmail.com -> lebron.gma-demo99 // already exists!
    //    lebron@gmail.com -> lebron.gma-demo100 // found! a feasible id
    //
    // c. in any cases(a or b), if generated Id is longer than FieldValidator.COURSE_ID_MAX_LENGTH, shorten the part
    //    before "@" of the initial input email, by continuously removing its last character

    /**
     * Generate a course ID for demo course, and if the generated id already exists, try another one.
     *
     * @param instructorEmail is the instructor email.
     * @return generated course id
     */
    private String generateDemoCourseId(String instructorEmail) {
        String proposedCourseId = generateNextDemoCourseId(instructorEmail, FieldValidator.COURSE_ID_MAX_LENGTH);
        while (logic.getCourse(proposedCourseId) != null) {
            proposedCourseId = generateNextDemoCourseId(proposedCourseId, FieldValidator.COURSE_ID_MAX_LENGTH);
        }
        return proposedCourseId;
    }

    /**
     * Generate a course ID for demo course from a given email.
     *
     * @param instructorEmail is the instructor email.
     * @return the first proposed course id. eg.lebron@gmail.com -> lebron.gma-demo
     */
    private String getDemoCourseIdRoot(String instructorEmail) {
        String[] emailSplit = instructorEmail.split("@");

        String username = emailSplit[0];
        String host = emailSplit[1];

        String head = StringHelper.replaceIllegalChars(username, FieldValidator.REGEX_COURSE_ID, '_');
        String hostAbbreviation = host.substring(0, Math.min(host.length(), 3));

        return head + "." + hostAbbreviation + "-demo";
    }

    /**
     * Generate a course ID for demo course from a given email or a generated course Id.
     *
     * <p>Here we check the input string is an email or course Id and handle them accordingly;
     * check the resulting course id, and if bigger than maximumIdLength, cut it so that it equals maximumIdLength.
     *
     * @param instructorEmailOrProposedCourseId is the instructor email or a proposed course id that already exists.
     * @param maximumIdLength is the maximum resulting id length allowed, above which we will cut the part before "@"
     * @return the proposed course id, e.g.:
     *         <ul>
     *         <li>lebron@gmail.com -> lebron.gma-demo</li>
     *         <li>lebron.gma-demo -> lebron.gma-demo0</li>
     *         <li>lebron.gma-demo0 -> lebron.gma-demo1</li>
     *         <li>012345678901234567890123456789.gma-demo9 -> 01234567890123456789012345678.gma-demo10 (being cut)</li>
     *         </ul>
     */
    private String generateNextDemoCourseId(String instructorEmailOrProposedCourseId, int maximumIdLength) {
        boolean isFirstCourseId = instructorEmailOrProposedCourseId.contains("@");
        if (isFirstCourseId) {
            return StringHelper.truncateHead(getDemoCourseIdRoot(instructorEmailOrProposedCourseId), maximumIdLength);
        }

        boolean isFirstTimeDuplicate = instructorEmailOrProposedCourseId.endsWith("-demo");
        if (isFirstTimeDuplicate) {
            return StringHelper.truncateHead(instructorEmailOrProposedCourseId + "0", maximumIdLength);
        }

        int lastIndexOfDemo = instructorEmailOrProposedCourseId.lastIndexOf("-demo");
        String root = instructorEmailOrProposedCourseId.substring(0, lastIndexOfDemo);
        int previousDedupSuffix = Integer.parseInt(instructorEmailOrProposedCourseId.substring(lastIndexOfDemo + 5));

        return StringHelper.truncateHead(root + "-demo" + (previousDedupSuffix + 1), maximumIdLength);
    }

    /**
     * Output format for {@link CreateAccountAction}.
     */
    public static class JoinLink extends ApiOutput {

        private final String joinLink;

        public JoinLink(String joinLink) {
            this.joinLink = joinLink;
        }

        public String getJoinLink() {
            return joinLink;
        }

    }

}
