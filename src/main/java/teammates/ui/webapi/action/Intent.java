package teammates.ui.webapi.action;

/**
 * The intent of calling the REST API.
 */
public enum Intent {

    /**
     * To get the full detail of the entities.
     */
    FULL_DETAIL,

    /**
     * To submit the feedback session as instructors.
     */
    INSTRUCTOR_SUBMISSION,

    /**
     * To submit the feedback session as students.
     */
    STUDENT_SUBMISSION,
}
