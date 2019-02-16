package teammates.ui.webapi.action;

import org.apache.http.HttpStatus;

import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.EmailSendingException;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.exception.TeammatesException;
import teammates.common.util.Const;
import teammates.common.util.EmailWrapper;
import teammates.common.util.Logger;

/**
 * Action: joins a course for a student/instructor.
 */
public class JoinCourseAction extends Action {

    private static final Logger log = Logger.getLogger();

    @Override
    protected AuthType getMinAuthLevel() {
        return AuthType.LOGGED_IN;
    }

    @Override
    public void checkSpecificAccessControl() {
        // Any user can use a join link as long as its parameters are valid
    }

    @Override
    public ActionResult execute() {
        String regkey = getNonNullRequestParamValue(Const.ParamsNames.REGKEY);
        String entityType = getNonNullRequestParamValue(Const.ParamsNames.ENTITY_TYPE);
        switch (entityType) {
        case Const.EntityType.STUDENT:
            return joinCourseForStudent(regkey);
        case Const.EntityType.INSTRUCTOR:
            String institute = getRequestParamValue(Const.ParamsNames.INSTRUCTOR_INSTITUTION);
            return joinCourseForInstructor(regkey, institute);
        default:
            return new JsonResult("Error: invalid entity type", HttpStatus.SC_BAD_REQUEST);
        }
    }

    private JsonResult joinCourseForStudent(String regkey) {
        StudentAttributes student;

        try {
            student = logic.joinCourseForStudent(regkey, userInfo.id);
        } catch (EntityDoesNotExistException ednee) {
            return new JsonResult(ednee.getMessage(), HttpStatus.SC_NOT_FOUND);
        } catch (EntityAlreadyExistsException eaee) {
            return new JsonResult(eaee.getMessage(), HttpStatus.SC_BAD_REQUEST);
        } catch (InvalidParametersException ipe) {
            return new JsonResult(ipe.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        sendJoinEmail(student.course, student.name, student.email, false);

        return new JsonResult("Student successfully joined course", HttpStatus.SC_OK);
    }

    private JsonResult joinCourseForInstructor(String regkey, String institute) {
        InstructorAttributes instructor;

        try {
            instructor = logic.joinCourseForInstructor(regkey, userInfo.id, institute);
        } catch (EntityDoesNotExistException ednee) {
            return new JsonResult(ednee.getMessage(), HttpStatus.SC_NOT_FOUND);
        } catch (EntityAlreadyExistsException eaee) {
            return new JsonResult(eaee.getMessage(), HttpStatus.SC_BAD_REQUEST);
        } catch (InvalidParametersException ipe) {
            return new JsonResult(ipe.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        sendJoinEmail(instructor.courseId, instructor.name, instructor.email, true);

        return new JsonResult("Instructor successfully joined course", HttpStatus.SC_OK);
    }

    private void sendJoinEmail(String courseId, String userName, String userEmail, boolean isInstructor) {
        CourseAttributes course = logic.getCourse(courseId);
        EmailWrapper email = emailGenerator.generateUserCourseRegisteredEmail(
                userName, userEmail, userInfo.id, isInstructor, course);
        try {
            emailSender.sendEmail(email);
        } catch (EmailSendingException e) {
            log.severe("User course register email failed to send: " + TeammatesException.toStringWithStackTrace(e));
        }
    }

}
