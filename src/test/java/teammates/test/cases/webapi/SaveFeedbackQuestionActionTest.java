package teammates.test.cases.webapi;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.http.HttpStatus;
import org.testng.annotations.Test;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.FeedbackSessionDetailsBundle;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.questions.FeedbackContributionQuestionDetails;
import teammates.common.datatransfer.questions.FeedbackQuestionType;
import teammates.common.datatransfer.questions.FeedbackTextQuestionDetails;
import teammates.common.exception.EntityNotFoundException;
import teammates.common.exception.InvalidHttpRequestBodyException;
import teammates.common.util.Const;
import teammates.common.util.JsonUtils;
import teammates.storage.api.FeedbackResponsesDb;
import teammates.ui.webapi.action.JsonResult;
import teammates.ui.webapi.action.SaveFeedbackQuestionAction;
import teammates.ui.webapi.output.FeedbackQuestionData;
import teammates.ui.webapi.output.FeedbackVisibilityType;
import teammates.ui.webapi.output.NumberOfEntitiesToGiveFeedbackToSetting;
import teammates.ui.webapi.request.FeedbackQuestionSaveRequest;

/**
 * SUT: {@link SaveFeedbackQuestionAction}.
 */
public class SaveFeedbackQuestionActionTest extends BaseActionTest<SaveFeedbackQuestionAction> {

    @Override
    protected String getActionUri() {
        return Const.ResourceURIs.QUESTION;
    }

    @Override
    protected String getRequestMethod() {
        return PUT;
    }

    @Override
    @Test
    protected void testExecute() throws Exception {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(session.getFeedbackSessionName(), session.getCourseId(), 1);
        assertEquals(FeedbackQuestionType.TEXT, typicalQuestion.getQuestionType());

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        ______TS("Not enough parameters");

        verifyHttpParameterFailure();

        ______TS("success: Typical case");

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };
        FeedbackQuestionSaveRequest saveRequest = getTypicalTextQuestionSaveRequest();

        SaveFeedbackQuestionAction a = getAction(saveRequest, param);
        JsonResult r = getJsonResult(a);

        assertEquals(HttpStatus.SC_OK, r.getStatusCode());
        FeedbackQuestionData response = (FeedbackQuestionData) r.getOutput();

        typicalQuestion = logic.getFeedbackQuestion(typicalQuestion.getId());
        assertEquals(typicalQuestion.getQuestionNumber(), response.getQuestionNumber());
        assertEquals(2, typicalQuestion.getQuestionNumber());

        assertEquals(typicalQuestion.getQuestionDetails().getQuestionText(), response.getQuestionBrief());
        assertEquals("this is the brief", typicalQuestion.getQuestionDetails().getQuestionText());

        assertEquals(typicalQuestion.getQuestionDescription(), response.getQuestionDescription());
        assertEquals("this is the description", typicalQuestion.getQuestionDescription());

        assertEquals(typicalQuestion.getQuestionType(), response.getQuestionType());
        assertEquals(FeedbackQuestionType.TEXT, typicalQuestion.getQuestionType());

        assertEquals(JsonUtils.toJson(typicalQuestion.getQuestionDetails()),
                JsonUtils.toJson(response.getQuestionDetails()));
        assertEquals(800, ((FeedbackTextQuestionDetails)
                typicalQuestion.getQuestionDetails()).getRecommendedLength());

        assertEquals(typicalQuestion.getGiverType(), typicalQuestion.getGiverType());
        assertEquals(FeedbackParticipantType.STUDENTS, typicalQuestion.getGiverType());

        assertEquals(typicalQuestion.getRecipientType(), typicalQuestion.getRecipientType());
        assertEquals(FeedbackParticipantType.INSTRUCTORS, typicalQuestion.getRecipientType());

        assertEquals(NumberOfEntitiesToGiveFeedbackToSetting.UNLIMITED,
                response.getNumberOfEntitiesToGiveFeedbackToSetting());
        assertEquals(Const.MAX_POSSIBLE_RECIPIENTS, typicalQuestion.getNumberOfEntitiesToGiveFeedbackTo());

        assertNull(response.getCustomNumberOfEntitiesToGiveFeedbackTo());

        assertTrue(response.getShowResponsesTo().isEmpty());
        assertTrue(typicalQuestion.getShowResponsesTo().isEmpty());
        assertTrue(response.getShowGiverNameTo().isEmpty());
        assertTrue(typicalQuestion.getShowGiverNameTo().isEmpty());
        assertTrue(response.getShowRecipientNameTo().isEmpty());
        assertTrue(typicalQuestion.getShowRecipientNameTo().isEmpty());
    }

    @Test
    public void testExecute_customerNumberOfRecipient_shouldSaveSuccessfully() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(session.getFeedbackSessionName(), session.getCourseId(), 1);

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };
        FeedbackQuestionSaveRequest saveRequest = getTypicalTextQuestionSaveRequest();
        saveRequest.setNumberOfEntitiesToGiveFeedbackToSetting(NumberOfEntitiesToGiveFeedbackToSetting.CUSTOM);
        saveRequest.setCustomNumberOfEntitiesToGiveFeedbackTo(10);

        SaveFeedbackQuestionAction a = getAction(saveRequest, param);
        JsonResult r = getJsonResult(a);

        assertEquals(HttpStatus.SC_OK, r.getStatusCode());
        typicalQuestion = logic.getFeedbackQuestion(typicalQuestion.getId());

        assertEquals(10, typicalQuestion.getNumberOfEntitiesToGiveFeedbackTo());
    }

    @Test
    public void testExecute_anonymousTeamSession_shouldSaveSuccessfully() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(session.getFeedbackSessionName(), session.getCourseId(), 1);

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };
        FeedbackQuestionSaveRequest saveRequest = getTypicalTextQuestionSaveRequest();
        saveRequest.setGiverType(FeedbackParticipantType.STUDENTS);
        saveRequest.setRecipientType(FeedbackParticipantType.TEAMS);
        saveRequest.setShowResponsesTo(Arrays.asList(FeedbackVisibilityType.RECIPIENT));
        saveRequest.setShowGiverNameTo(Arrays.asList());
        saveRequest.setShowRecipientNameTo(Arrays.asList(FeedbackVisibilityType.RECIPIENT));

        SaveFeedbackQuestionAction a = getAction(saveRequest, param);
        JsonResult r = getJsonResult(a);

        assertEquals(HttpStatus.SC_OK, r.getStatusCode());
        typicalQuestion = logic.getFeedbackQuestion(typicalQuestion.getId());

        assertEquals(FeedbackParticipantType.STUDENTS, typicalQuestion.getGiverType());
        assertEquals(FeedbackParticipantType.TEAMS, typicalQuestion.getRecipientType());
        assertEquals(Arrays.asList(FeedbackParticipantType.RECEIVER), typicalQuestion.getShowResponsesTo());
        assertTrue(typicalQuestion.getShowGiverNameTo().isEmpty());
        assertEquals(Arrays.asList(FeedbackParticipantType.RECEIVER), typicalQuestion.getShowRecipientNameTo());
    }

    @Test
    public void testExecute_selfFeedback_shouldSaveSuccessfully() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(session.getFeedbackSessionName(), session.getCourseId(), 1);

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };
        FeedbackQuestionSaveRequest saveRequest = getTypicalTextQuestionSaveRequest();
        saveRequest.setGiverType(FeedbackParticipantType.STUDENTS);
        saveRequest.setRecipientType(FeedbackParticipantType.SELF);
        saveRequest.setShowResponsesTo(Arrays.asList(FeedbackVisibilityType.RECIPIENT));
        saveRequest.setShowGiverNameTo(Arrays.asList());
        saveRequest.setShowRecipientNameTo(Arrays.asList(FeedbackVisibilityType.RECIPIENT));

        SaveFeedbackQuestionAction a = getAction(saveRequest, param);
        JsonResult r = getJsonResult(a);

        assertEquals(HttpStatus.SC_OK, r.getStatusCode());
        typicalQuestion = logic.getFeedbackQuestion(typicalQuestion.getId());

        assertEquals(FeedbackParticipantType.STUDENTS, typicalQuestion.getGiverType());
        assertEquals(FeedbackParticipantType.SELF, typicalQuestion.getRecipientType());
        assertEquals(Arrays.asList(FeedbackParticipantType.RECEIVER), typicalQuestion.getShowResponsesTo());
        assertTrue(typicalQuestion.getShowGiverNameTo().isEmpty());
        assertEquals(Arrays.asList(FeedbackParticipantType.RECEIVER), typicalQuestion.getShowRecipientNameTo());
    }

    @Test
    public void testExecute_editingContributionTypeQuestion_shouldSaveSuccessfully() {
        DataBundle dataBundle = loadDataBundle("/FeedbackSessionQuestionTypeTest.json");
        removeAndRestoreDataBundle(dataBundle);

        InstructorAttributes instructor1ofCourse1 = dataBundle.instructors.get("instructor1OfCourse1");

        loginAsInstructor(instructor1ofCourse1.googleId);

        FeedbackSessionAttributes fs = dataBundle.feedbackSessions.get("contribSession");
        FeedbackQuestionAttributes fq =
                logic.getFeedbackQuestion(fs.getFeedbackSessionName(), fs.getCourseId(), 1);
        FeedbackResponsesDb frDb = new FeedbackResponsesDb();

        ______TS("Edit text won't delete response");

        // There are already responses for this question
        assertFalse(frDb.getFeedbackResponsesForQuestion(fq.getId()).isEmpty());

        FeedbackQuestionSaveRequest saveRequest = getTypicalContributionQuestionSaveRequest();
        saveRequest.setQuestionNumber(fq.getQuestionNumber());
        saveRequest.setGiverType(fq.getGiverType());
        saveRequest.setRecipientType(fq.getRecipientType());
        saveRequest.setQuestionDetails(fq.getQuestionDetails());

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, fq.getFeedbackQuestionId(),
        };
        SaveFeedbackQuestionAction a = getAction(saveRequest, param);
        JsonResult r = getJsonResult(a);

        assertEquals(HttpStatus.SC_OK, r.getStatusCode());

        // All existing responses should remain
        assertFalse(frDb.getFeedbackResponsesForQuestion(fq.getId()).isEmpty());

        ______TS("Edit: Invalid recipient type");

        assertThrows(InvalidHttpRequestBodyException.class, () -> {
            FeedbackQuestionSaveRequest request = getTypicalContributionQuestionSaveRequest();
            request.setQuestionNumber(fq.getQuestionNumber());
            request.setRecipientType(FeedbackParticipantType.STUDENTS);
            getJsonResult(getAction(request, param));
        });
    }

    @Test
    public void testExecute_invalidQuestionNumber_shouldThrowException() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(session.getFeedbackSessionName(), session.getCourseId(), 1);

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };
        FeedbackQuestionSaveRequest saveRequest = getTypicalTextQuestionSaveRequest();
        saveRequest.setQuestionNumber(-1);

        SaveFeedbackQuestionAction a = getAction(saveRequest, param);

        assertThrows(InvalidHttpRequestBodyException.class, () -> {
            getJsonResult(a);
        });

        // question is not updated
        assertEquals(typicalQuestion.getQuestionDescription(),
                logic.getFeedbackQuestion(typicalQuestion.getId()).getQuestionDescription());
    }

    @Test
    public void testExecute_invalidGiverRecipientType_shouldThrowException() {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes session = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(session.getFeedbackSessionName(), session.getCourseId(), 1);

        loginAsInstructor(instructor1ofCourse1.getGoogleId());

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };
        FeedbackQuestionSaveRequest saveRequest = getTypicalTextQuestionSaveRequest();
        saveRequest.setGiverType(FeedbackParticipantType.TEAMS);
        saveRequest.setRecipientType(FeedbackParticipantType.OWN_TEAM_MEMBERS);

        SaveFeedbackQuestionAction a = getAction(saveRequest, param);

        assertThrows(InvalidHttpRequestBodyException.class, () -> {
            getJsonResult(a);
        });

        // question is not updated
        assertEquals(typicalQuestion.getQuestionDescription(),
                logic.getFeedbackQuestion(typicalQuestion.getId()).getQuestionDescription());
    }

    @Test
    public void testExecute_differentScenarios_shouldUpdateResponseRateCorrectly() throws Exception {
        InstructorAttributes instructor1ofCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes fs = typicalBundle.feedbackSessions.get("session1InCourse1");

        int numStudentRespondents = 4;
        int numInstructorRespondents = 1;

        int totalStudents = 5;
        int totalInstructors = 5;

        loginAsInstructor(instructor1ofCourse1.googleId);

        ______TS("Check response rate before editing question 1");

        fs = logic.getFeedbackSession(fs.getFeedbackSessionName(), fs.getCourseId());
        FeedbackSessionDetailsBundle details =
                logic.getFeedbackSessionDetails(fs.getFeedbackSessionName(), fs.getCourseId());
        assertEquals(numStudentRespondents + numInstructorRespondents, details.stats.submittedTotal);
        assertEquals(totalStudents + totalInstructors, details.stats.expectedTotal);

        ______TS("Change the feedback path of a question with no unique respondents, "
                + "response rate should not be updated");

        FeedbackQuestionAttributes fq =
                logic.getFeedbackQuestion(fs.getFeedbackSessionName(), fs.getCourseId(), 1);
        FeedbackQuestionSaveRequest saveRequest = getTypicalTextQuestionSaveRequest();
        saveRequest.setQuestionNumber(fq.getQuestionNumber());
        saveRequest.setGiverType(FeedbackParticipantType.STUDENTS);
        saveRequest.setRecipientType(FeedbackParticipantType.STUDENTS);
        saveRequest.setNumberOfEntitiesToGiveFeedbackToSetting(NumberOfEntitiesToGiveFeedbackToSetting.CUSTOM);
        saveRequest.setCustomNumberOfEntitiesToGiveFeedbackTo(1);

        String[] param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, fq.getFeedbackQuestionId(),
        };
        SaveFeedbackQuestionAction a = getAction(saveRequest, param);
        getJsonResult(a);

        // TODO first comment was there before, but the second one seems to be the one happening?
        // Response rate should not change because other questions have the same respondents
        // Response rate should decrease by 1 as response from student1 in qn1 is changed
        numStudentRespondents--;
        fs = logic.getFeedbackSession(fs.getFeedbackSessionName(), fs.getCourseId());
        details = logic.getFeedbackSessionDetails(fs.getFeedbackSessionName(), fs.getCourseId());
        assertEquals(numStudentRespondents + numInstructorRespondents, details.stats.submittedTotal);
        assertEquals(totalStudents + totalInstructors, details.stats.expectedTotal);

        ______TS("Change the feedback path of a question with a unique instructor respondent, "
                + "response rate changed");

        fq = logic.getFeedbackQuestion(fs.getFeedbackSessionName(), fs.getCourseId(), 3);
        saveRequest = getTypicalTextQuestionSaveRequest();
        saveRequest.setQuestionNumber(fq.getQuestionNumber());
        saveRequest.setGiverType(fq.getGiverType());
        saveRequest.setRecipientType(FeedbackParticipantType.STUDENTS);

        param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, fq.getFeedbackQuestionId(),
        };
        a = getAction(saveRequest, param);
        getJsonResult(a);

        // Response rate should decrease by 1 because the response of the unique instructor respondent is deleted
        fs = logic.getFeedbackSession(fs.getFeedbackSessionName(), fs.getCourseId());
        details = logic.getFeedbackSessionDetails(fs.getFeedbackSessionName(), fs.getCourseId());
        assertEquals(numStudentRespondents, details.stats.submittedTotal);
        assertEquals(totalStudents + totalInstructors, details.stats.expectedTotal);

        ______TS("Change the feedback path of a question so that some possible respondents are removed");

        fq = logic.getFeedbackQuestion(fs.getFeedbackSessionName(), fs.getCourseId(), 4);
        saveRequest = getTypicalTextQuestionSaveRequest();
        saveRequest.setQuestionNumber(fq.getQuestionNumber());
        saveRequest.setGiverType(FeedbackParticipantType.STUDENTS);
        saveRequest.setRecipientType(FeedbackParticipantType.NONE);

        param = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, fq.getFeedbackQuestionId(),
        };
        a = getAction(saveRequest, param);
        getJsonResult(a);

        // Total possible respondents should decrease because instructors
        // (except session creator) are no longer possible respondents
        fs = logic.getFeedbackSession(fs.getFeedbackSessionName(), fs.getCourseId());
        details = logic.getFeedbackSessionDetails(fs.getFeedbackSessionName(), fs.getCourseId());
        assertEquals(numStudentRespondents, details.stats.submittedTotal);
        assertEquals(totalStudents + 1, details.stats.expectedTotal);
    }

    private FeedbackQuestionSaveRequest getTypicalTextQuestionSaveRequest() {
        FeedbackQuestionSaveRequest saveRequest = new FeedbackQuestionSaveRequest();
        saveRequest.setQuestionNumber(2);
        saveRequest.setQuestionBrief("this is the brief");
        saveRequest.setQuestionDescription("this is the description");
        FeedbackTextQuestionDetails textQuestionDetails = new FeedbackTextQuestionDetails();
        textQuestionDetails.setRecommendedLength(800);
        saveRequest.setQuestionDetails(textQuestionDetails);
        saveRequest.setQuestionType(FeedbackQuestionType.TEXT);
        saveRequest.setGiverType(FeedbackParticipantType.STUDENTS);
        saveRequest.setRecipientType(FeedbackParticipantType.INSTRUCTORS);
        saveRequest.setNumberOfEntitiesToGiveFeedbackToSetting(NumberOfEntitiesToGiveFeedbackToSetting.UNLIMITED);

        saveRequest.setShowResponsesTo(new ArrayList<>());
        saveRequest.setShowGiverNameTo(new ArrayList<>());
        saveRequest.setShowRecipientNameTo(new ArrayList<>());

        return saveRequest;
    }

    private FeedbackQuestionSaveRequest getTypicalContributionQuestionSaveRequest() {
        FeedbackQuestionSaveRequest saveRequest = new FeedbackQuestionSaveRequest();
        saveRequest.setQuestionNumber(1);
        saveRequest.setQuestionBrief("this is the brief for contribution question");
        saveRequest.setQuestionDescription("this is the description for contribution question");
        FeedbackContributionQuestionDetails textQuestionDetails = new FeedbackContributionQuestionDetails();
        textQuestionDetails.setNotSureAllowed(false);
        saveRequest.setQuestionDetails(textQuestionDetails);
        saveRequest.setQuestionType(FeedbackQuestionType.CONTRIB);
        saveRequest.setGiverType(FeedbackParticipantType.STUDENTS);
        saveRequest.setRecipientType(FeedbackParticipantType.OWN_TEAM_MEMBERS_INCLUDING_SELF);
        saveRequest.setNumberOfEntitiesToGiveFeedbackToSetting(NumberOfEntitiesToGiveFeedbackToSetting.UNLIMITED);

        saveRequest.setShowResponsesTo(Arrays.asList(FeedbackVisibilityType.INSTRUCTORS));
        saveRequest.setShowGiverNameTo(Arrays.asList(FeedbackVisibilityType.INSTRUCTORS));
        saveRequest.setShowRecipientNameTo(Arrays.asList(FeedbackVisibilityType.INSTRUCTORS));

        return saveRequest;
    }

    @Override
    @Test
    protected void testAccessControl() throws Exception {
        InstructorAttributes instructor1OfCourse1 = typicalBundle.instructors.get("instructor1OfCourse1");
        FeedbackSessionAttributes fs = typicalBundle.feedbackSessions.get("session1InCourse1");
        FeedbackQuestionAttributes typicalQuestion =
                logic.getFeedbackQuestion(fs.getFeedbackSessionName(), fs.getCourseId(), 1);

        ______TS("non-existent feedback question");

        loginAsInstructor(instructor1OfCourse1.googleId);

        assertThrows(EntityNotFoundException.class, () -> {
            getAction(new String[] {Const.ParamsNames.FEEDBACK_QUESTION_ID, "random"}).checkSpecificAccessControl();
        });

        ______TS("inaccessible without ModifySessionPrivilege");

        String[] submissionParams = new String[] {
                Const.ParamsNames.FEEDBACK_QUESTION_ID, typicalQuestion.getFeedbackQuestionId(),
        };

        verifyInaccessibleWithoutModifySessionPrivilege(submissionParams);

        ______TS("only instructors of the same course can access");

        verifyOnlyInstructorsOfTheSameCourseCanAccess(submissionParams);
    }

}
