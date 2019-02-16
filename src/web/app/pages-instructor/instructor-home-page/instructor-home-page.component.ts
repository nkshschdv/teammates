import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FeedbackQuestionsService } from '../../../services/feedback-questions.service';
import { FeedbackSessionsService } from '../../../services/feedback-sessions.service';
import { HttpRequestService } from '../../../services/http-request.service';
import { NavigationService } from '../../../services/navigation.service';
import { StatusMessageService } from '../../../services/status-message.service';
import { TimezoneService } from '../../../services/timezone.service';
import { FeedbackSession, FeedbackSessions, MessageOutput } from '../../../types/api-output';
import {
  CopySessionResult,
  SessionsTableColumn,
  SessionsTableHeaderColorScheme,
  SessionsTableRowModel,
  SortBy,
  SortOrder,
} from '../../components/sessions-table/sessions-table-model';
import { Course, Courses } from '../../course';
import { ErrorMessageOutput } from '../../error-message-output';
import { defaultInstructorPrivilege, InstructorPrivilege } from '../../instructor-privilege';
import { InstructorSessionBasePageComponent } from '../instructor-session-base-page.component';

interface CourseTabModel {
  course: Course;
  instructorPrivilege: InstructorPrivilege;
  sessionsTableRowModels: SessionsTableRowModel[];
  sessionsTableRowModelsSortBy: SortBy;
  sessionsTableRowModelsSortOrder: SortOrder;

  isTabExpanded: boolean;
}

/**
 * Instructor home page.
 */
@Component({
  selector: 'tm-instructor-home-page',
  templateUrl: './instructor-home-page.component.html',
  styleUrls: ['./instructor-home-page.component.scss'],
})
export class InstructorHomePageComponent extends InstructorSessionBasePageComponent implements OnInit {

  // enum
  SessionsTableColumn: typeof SessionsTableColumn = SessionsTableColumn;
  SessionsTableHeaderColorScheme: typeof SessionsTableHeaderColorScheme = SessionsTableHeaderColorScheme;
  SortBy: typeof SortBy = SortBy;

  user: string = '';
  studentSearchkey: string = '';
  instructorCoursesSortBy: SortBy = SortBy.CREATION_DATE;

  // data
  courseTabModels: CourseTabModel[] = [];

  constructor(router: Router, httpRequestService: HttpRequestService,
              statusMessageService: StatusMessageService, navigationService: NavigationService,
              feedbackSessionsService: FeedbackSessionsService, feedbackQuestionsService: FeedbackQuestionsService,
              private route: ActivatedRoute, private ngbModal: NgbModal, private timezoneService: TimezoneService) {
    super(router, httpRequestService, statusMessageService, navigationService,
        feedbackSessionsService, feedbackQuestionsService);
    // need timezone data for moment()
    this.timezoneService.getTzVersion();
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe((queryParams: any) => {
      this.user = queryParams.user;

      this.loadCourses();
    });
  }

  /**
   * Gets a list of courses belong to current user.
   */
  get courseCandidates(): Course[] {
    return this.courseTabModels.map((m: CourseTabModel) => m.course);
  }

  /**
   * Redirect to the search page and query the search
   */
  search(): void {
    this.router.navigate(['web/instructor/search'], {
      queryParams: { studentSearchkey: this.studentSearchkey },
    });
  }

  /**
   * Open the modal for different buttons and link.
   */
  openModal(content: any): void {
    this.ngbModal.open(content);
  }

  /**
   * Archives the entire course from the instructor
   */
  archiveCourse(courseId: string): void {
    this.httpRequestService.put('/course', { courseid: courseId, archive: 'true' })
      .subscribe((resp: MessageOutput) => {
        this.loadCourses();
        this.statusMessageService.showSuccessMessage(resp.message);
      }, (resp: ErrorMessageOutput) => {
        this.statusMessageService.showErrorMessage(resp.error.message);
      });
  }

  /**
   * Deletes the entire course from the instructor
   */
  deleteCourse(courseId: string): void {
    this.httpRequestService.delete('/course', { courseid: courseId })
      .subscribe((resp: MessageOutput) => {
        this.loadCourses();
        this.statusMessageService.showSuccessMessage(resp.message);
      }, (resp: ErrorMessageOutput) => {
        this.statusMessageService.showErrorMessage(resp.error.message);
      });
  }
  /**
   * Loads courses of current instructor.
   */
  loadCourses(): void {
    this.courseTabModels = [];
    this.httpRequestService.get('/courses').subscribe((courses: Courses) => {
      courses.courses.forEach((course: Course) => {
        const model: CourseTabModel = {
          course,
          instructorPrivilege: defaultInstructorPrivilege,
          sessionsTableRowModels: [],
          isTabExpanded: false,
          sessionsTableRowModelsSortBy: SortBy.NONE,
          sessionsTableRowModelsSortOrder: SortOrder.ASC,
        };

        this.courseTabModels.push(model);
        this.updateCourseInstructorPrivilege(model);
        this.loadFeedbackSessions(model);
      });
    }, (resp: ErrorMessageOutput) => { this.statusMessageService.showErrorMessage(resp.error.message); });
  }

  /**
   * Updates the instructor privilege in {@code CourseTabModel}.
   */
  updateCourseInstructorPrivilege(model: CourseTabModel): void {
    this.httpRequestService.get('/instructor/privilege', {
      courseid: model.course.courseId,
    }).subscribe((instructorPrivilege: InstructorPrivilege) => {
      model.instructorPrivilege = instructorPrivilege;
    }, (resp: ErrorMessageOutput) => {
      this.statusMessageService.showErrorMessage(resp.error.message);
    });
  }

  /**
   * Loads the feedback session in the course.
   */
  loadFeedbackSessions(model: CourseTabModel): void {
    this.httpRequestService.get('/sessions', {
      courseid: model.course.courseId,
    }).subscribe((response: FeedbackSessions) => {
      response.feedbackSessions.forEach((feedbackSession: FeedbackSession) => {
        const m: SessionsTableRowModel = {
          feedbackSession,
          responseRate: '',
          isLoadingResponseRate: false,
          instructorPrivilege: defaultInstructorPrivilege,
        };
        model.sessionsTableRowModels.push(m);
        this.updateInstructorPrivilege(m);
      });

      model.isTabExpanded = true;
    }, (resp: ErrorMessageOutput) => { this.statusMessageService.showErrorMessage(resp.error.message); });
  }

  /**
   * Checks the option selected to sort courses.
   */
  isSelectedForSorting(by: SortBy): boolean {
    return this.instructorCoursesSortBy === by;
  }

  /**
   * Sorts the courses according to selected option.
   */
  sortCoursesBy(by: SortBy): void {
    this.instructorCoursesSortBy = by;

    if (this.courseTabModels.length > 1) {
      this.courseTabModels.sort(this.sortPanelsBy(by));
    }
  }

  /**
   * Sorts the panels of courses in order.
   */
  sortPanelsBy(by: SortBy):
      ((a: { course: Course }, b: { course: Course }) => number) {
    return ((a: { course: Course }, b: { course: Course }): number => {
      let strA: string;
      let strB: string;
      switch (by) {
        case SortBy.COURSE_NAME:
          strA = a.course.courseName;
          strB = b.course.courseName;
          break;
        case SortBy.COURSE_ID:
          strA = a.course.courseId;
          strB = b.course.courseId;
          break;
        case SortBy.CREATION_DATE:
          strA = a.course.creationDate;
          strB = b.course.creationDate;
          break;
        default:
          strA = '';
          strB = '';
      }
      return strA.localeCompare(strB);
    });
  }

  /**
   * Sorts the list of feedback session row.
   */
  sortSessionsTableRowModelsEvent(tabIndex: number, by: SortBy): void {
    const tab: CourseTabModel = this.courseTabModels[tabIndex];

    tab.sessionsTableRowModelsSortBy = by;
    // reverse the sort order
    tab.sessionsTableRowModelsSortOrder =
        tab.sessionsTableRowModelsSortOrder === SortOrder.DESC ? SortOrder.ASC : SortOrder.DESC;
    tab.sessionsTableRowModels.sort(this.sortModelsBy(by, tab.sessionsTableRowModelsSortOrder));
  }

  /**
   * Loads response rate of a feedback session.
   */
  loadResponseRateEventHandler(tabIndex: number, rowIndex: number): void {
    this.loadResponseRate(this.courseTabModels[tabIndex].sessionsTableRowModels[rowIndex]);
  }

  /**
   * Edits the feedback session.
   */
  editSessionEventHandler(tabIndex: number, rowIndex: number): void {
    this.editSession(this.courseTabModels[tabIndex].sessionsTableRowModels[rowIndex]);
  }

  /**
   * Moves the feedback session to the recycle bin.
   */
  moveSessionToRecycleBinEventHandler(tabIndex: number, rowIndex: number): void {
    const model: SessionsTableRowModel = this.courseTabModels[tabIndex].sessionsTableRowModels[rowIndex];
    const paramMap: { [key: string]: string } = {
      courseid: model.feedbackSession.courseId,
      fsname: model.feedbackSession.feedbackSessionName,
    };

    this.httpRequestService.put('/bin/session', paramMap)
        .subscribe(() => {
          this.courseTabModels[tabIndex].sessionsTableRowModels.splice(
              this.courseTabModels[tabIndex].sessionsTableRowModels.indexOf(model), 1);
          this.statusMessageService.showSuccessMessage(
              "The feedback session has been deleted. You can restore it from the 'Sessions' tab.");
        }, (resp: ErrorMessageOutput) => { this.statusMessageService.showErrorMessage(resp.error.message); });
  }

  /**
   * Edits the feedback session.
   */
  copySessionEventHandler(tabIndex: number, result: CopySessionResult): void {
    this.copySession(this.courseTabModels[tabIndex].sessionsTableRowModels[result.sessionToCopyRowIndex], result);
  }

  /**
   * Submits the feedback session as instructor.
   */
  submitSessionAsInstructorEventHandler(tabIndex: number, rowIndex: number): void {
    this.submitSessionAsInstructor(this.courseTabModels[tabIndex].sessionsTableRowModels[rowIndex]);
  }

  /**
   * Views the result of a feedback session.
   */
  viewSessionResultEventHandler(): void {
    this.viewSessionResult();
  }

  /**
   * Publishes a feedback session.
   */
  publishSessionEventHandler(tabIndex: number, rowIndex: number): void {
    this.publishSession(this.courseTabModels[tabIndex].sessionsTableRowModels[rowIndex]);
  }

  /**
   * Unpublishes a feedback session.
   */
  unpublishSessionEventHandler(tabIndex: number, rowIndex: number): void {
    this.unpublishSession(this.courseTabModels[tabIndex].sessionsTableRowModels[rowIndex]);
  }

  /**
   * Sends e-mails to remind students on the published results link.
   */
  resendResultsLinkToStudentsEventHandler(tabIndex: number, remindInfo: any): void {
    this.resendResultsLinkToStudents(this.courseTabModels[tabIndex]
        .sessionsTableRowModels[remindInfo.row], remindInfo.request);
  }

  /**
   * Sends e-mails to remind students who have not submitted their feedback.
   */
  sendRemindersToStudentsEventHandler(tabIndex: number, remindInfo: any): void {
    this.sendRemindersToStudents(this.courseTabModels[tabIndex]
      .sessionsTableRowModels[remindInfo.row], remindInfo.request);
  }
}
