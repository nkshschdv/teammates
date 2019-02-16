package teammates.ui.webapi.action;

import java.time.Instant;

import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.attributes.StudentAttributes;
import teammates.common.exception.EmailSendingException;
import teammates.common.exception.InvalidHttpParameterException;
import teammates.common.exception.TeammatesException;
import teammates.common.util.Const;
import teammates.common.util.EmailWrapper;
import teammates.common.util.Logger;
import teammates.logic.api.EmailGenerator;
import teammates.ui.webapi.output.ApiOutput;

/**
 * Confirm the submission of a feedback session.
 */
public class ConfirmFeedbackSessionSubmissionAction extends BasicFeedbackSubmissionAction {

    private static final Logger log = Logger.getLogger();

    @Override
    protected AuthType getMinAuthLevel() {
        return AuthType.PUBLIC;
    }

    @Override
    public void checkSpecificAccessControl() {
        String courseId = getNonNullRequestParamValue(Const.ParamsNames.COURSE_ID);
        String feedbackSessionName = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);
        FeedbackSessionAttributes feedbackSession = logic.getFeedbackSession(feedbackSessionName, courseId);

        verifySessionOpenExceptForModeration(feedbackSession);
        verifyNotPreview();

        Intent intent = Intent.valueOf(getNonNullRequestParamValue(Const.ParamsNames.INTENT));
        switch (intent) {
        case STUDENT_SUBMISSION:
            StudentAttributes studentAttributes = getStudentOfCourseFromRequest(feedbackSession.getCourseId());
            checkAccessControlForStudentFeedbackSubmission(studentAttributes, feedbackSession);
            break;
        case INSTRUCTOR_SUBMISSION:
            InstructorAttributes instructorAttributes = getInstructorOfCourseFromRequest(feedbackSession.getCourseId());
            checkAccessControlForInstructorFeedbackSubmission(instructorAttributes, feedbackSession);
            break;
        default:
            throw new InvalidHttpParameterException("Unknown intent " + intent);
        }
    }

    @Override
    public ActionResult execute() {
        String courseId = getNonNullRequestParamValue(Const.ParamsNames.COURSE_ID);
        String feedbackSessionName = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);
        FeedbackSessionAttributes feedbackSession = logic.getFeedbackSession(feedbackSessionName, courseId);
        boolean isSubmissionEmailConfirmationEmailRequested =
                getBooleanRequestParamValue(Const.ParamsNames.SEND_SUBMISSION_EMAIL);
        Intent intent = Intent.valueOf(getNonNullRequestParamValue(Const.ParamsNames.INTENT));

        EmailWrapper email = null;
        switch (intent) {
        case STUDENT_SUBMISSION:
            StudentAttributes studentAttributes = getStudentOfCourseFromRequest(feedbackSession.getCourseId());
            boolean hasStudentRespondedForSession =
                    logic.hasGiverRespondedForSession(studentAttributes.getEmail(), feedbackSessionName, courseId);
            if (hasStudentRespondedForSession) {
                taskQueuer.scheduleUpdateRespondentForSession(
                        courseId, feedbackSessionName, studentAttributes.getEmail(), false, false);
            } else {
                taskQueuer.scheduleUpdateRespondentForSession(
                        courseId, feedbackSessionName, studentAttributes.getEmail(), false, true);
            }
            if (isSubmissionEmailConfirmationEmailRequested) {
                email = new EmailGenerator().generateFeedbackSubmissionConfirmationEmailForStudent(
                            feedbackSession, studentAttributes, Instant.now());
            }
            break;
        case INSTRUCTOR_SUBMISSION:
            InstructorAttributes instructorAttributes = getInstructorOfCourseFromRequest(feedbackSession.getCourseId());
            boolean hasInstructorRespondedForSession =
                    logic.hasGiverRespondedForSession(instructorAttributes.getEmail(), feedbackSessionName, courseId);
            if (hasInstructorRespondedForSession) {
                taskQueuer.scheduleUpdateRespondentForSession(
                        courseId, feedbackSessionName, instructorAttributes.getEmail(), true, false);
            } else {
                taskQueuer.scheduleUpdateRespondentForSession(
                        courseId, feedbackSessionName, instructorAttributes.getEmail(), true, true);
            }
            if (isSubmissionEmailConfirmationEmailRequested) {
                email = new EmailGenerator().generateFeedbackSubmissionConfirmationEmailForInstructor(
                        feedbackSession, instructorAttributes, Instant.now());
            }
            break;
        default:
            throw new InvalidHttpParameterException("Unknown intent " + intent);
        }

        if (email != null) {
            try {
                emailSender.sendEmail(email);
            } catch (EmailSendingException e) {
                log.severe("Submission confirmation email failed to send: "
                        + TeammatesException.toStringWithStackTrace(e));
                return new JsonResult(new ConfirmationResponse(ConfirmationResult.SUCCESS_BUT_EMAIL_FAIL_TO_SEND,
                        "Submission confirmation email failed to send"));
            }
        }

        return new JsonResult(new ConfirmationResponse(ConfirmationResult.SUCCESS, "Submission confirmed"));
    }

    /**
     * The result of the confirmation.
     */
    enum ConfirmationResult {
        SUCCESS,
        SUCCESS_BUT_EMAIL_FAIL_TO_SEND
    }

    /**
     * The output format of {@link ConfirmFeedbackSessionSubmissionAction}.
     */
    public static class ConfirmationResponse extends ApiOutput {
        private final ConfirmationResult result;
        private final String message;

        public ConfirmationResponse(ConfirmationResult result, String message) {
            this.result = result;
            this.message = message;
        }

        public ConfirmationResult getResult() {
            return result;
        }

        public String getMessage() {
            return message;
        }
    }
}
