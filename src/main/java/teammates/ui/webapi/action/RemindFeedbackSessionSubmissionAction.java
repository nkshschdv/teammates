package teammates.ui.webapi.action;

import org.apache.http.HttpStatus;

import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.util.Const;
import teammates.ui.webapi.request.FeedbackSessionStudentRemindRequest;

/**
 * Remind students about the feedback submission.
 */
public class RemindFeedbackSessionSubmissionAction extends Action {

    @Override
    protected AuthType getMinAuthLevel() {
        return AuthType.LOGGED_IN;
    }

    @Override
    public void checkSpecificAccessControl() {
        String courseId = getNonNullRequestParamValue(Const.ParamsNames.COURSE_ID);
        String feedbackSessionName = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);

        FeedbackSessionAttributes feedbackSession = logic.getFeedbackSession(feedbackSessionName, courseId);

        gateKeeper.verifyAccessible(
                logic.getInstructorForGoogleId(courseId, userInfo.getId()),
                feedbackSession,
                Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION);
    }

    @Override
    public ActionResult execute() {
        String courseId = getNonNullRequestParamValue(Const.ParamsNames.COURSE_ID);
        String feedbackSessionName = getNonNullRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);

        FeedbackSessionAttributes feedbackSession = logic.getFeedbackSession(feedbackSessionName, courseId);
        if (!feedbackSession.isOpened()) {
            return new JsonResult("Reminder email could not be sent out "
                    + "as the feedback session is not open for submissions.", HttpStatus.SC_BAD_REQUEST);
        }

        FeedbackSessionStudentRemindRequest remindRequest =
                getAndValidateRequestBody(FeedbackSessionStudentRemindRequest.class);
        String[] usersToRemind = remindRequest.getUsersToRemind();

        taskQueuer.scheduleFeedbackSessionRemindersForParticularUsers(courseId, feedbackSessionName,
                usersToRemind, userInfo.getId());

        return new JsonResult("Reminders sent");
    }

}
