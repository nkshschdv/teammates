package teammates.test.cases.automated;

import org.testng.annotations.Test;

import teammates.common.datatransfer.attributes.AccountAttributes;
import teammates.common.datatransfer.attributes.CourseAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.util.Const;
import teammates.common.util.Const.ParamsNames;
import teammates.common.util.EmailType;
import teammates.common.util.EmailWrapper;
import teammates.logic.core.AccountsLogic;
import teammates.ui.automated.InstructorCourseJoinEmailWorkerAction;

/**
 * SUT: {@link InstructorCourseJoinEmailWorkerAction}.
 */
public class InstructorCourseJoinEmailWorkerActionTest
        extends BaseAutomatedActionTest<InstructorCourseJoinEmailWorkerAction> {

    @Override
    protected String getActionUri() {
        return Const.TaskQueue.INSTRUCTOR_COURSE_JOIN_EMAIL_WORKER_URL;
    }

    @Test
    public void allTests() {

        CourseAttributes course1 = dataBundle.courses.get("typicalCourse1");
        InstructorAttributes instr1InCourse1 = dataBundle.instructors.get("instructor1OfCourse1");
        AccountAttributes inviter = AccountsLogic.inst().getAccount("idOfInstructor2OfCourse1");

        ______TS("typical case: new instructor joining");

        String[] submissionParams = new String[] {
                ParamsNames.COURSE_ID, course1.getId(),
                ParamsNames.INSTRUCTOR_EMAIL, instr1InCourse1.email,
                ParamsNames.INVITER_ID, inviter.googleId,
                ParamsNames.IS_INSTRUCTOR_REJOINING, "false",
        };

        InstructorCourseJoinEmailWorkerAction action = getAction(submissionParams);
        action.execute();

        verifyNumberOfEmailsSent(action, 1);

        EmailWrapper email = action.getEmailSender().getEmailsSent().get(0);

        assertEquals(String.format(EmailType.INSTRUCTOR_COURSE_JOIN.getSubject(), course1.getName(),
                                   course1.getId()),
                     email.getSubject());
        assertEquals(instr1InCourse1.email, email.getRecipient());

        ______TS("typical case: old instructor rejoining (after google id reset)");

        submissionParams = new String[] {
                ParamsNames.COURSE_ID, course1.getId(),
                ParamsNames.INSTRUCTOR_EMAIL, instr1InCourse1.email,
                ParamsNames.INSTRUCTOR_INSTITUTION, "Test Institute",
                ParamsNames.IS_INSTRUCTOR_REJOINING, "true",
        };

        action = getAction(submissionParams);
        action.execute();

        verifyNumberOfEmailsSent(action, 1);

        email = action.getEmailSender().getEmailsSent().get(0);

        assertEquals(String.format(EmailType.INSTRUCTOR_COURSE_REJOIN_AFTER_GOOGLE_ID_RESET.getSubject(), course1.getName(),
                course1.getId()),
                email.getSubject());
        assertEquals(instr1InCourse1.email, email.getRecipient());

    }

}
