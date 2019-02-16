import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { FeedbackSessionPublishStatus, FeedbackSessionSubmissionStatus } from '../../../types/api-output';
import { FeedbackSessionStudentRemindRequest } from '../../../types/api-request';
import { Course } from '../../course';
import { CopySessionModalResult } from '../copy-session-modal/copy-session-modal-model';
import { CopySessionModalComponent } from '../copy-session-modal/copy-session-modal.component';
import {
  ConfirmPublishingSessionModalComponent,
} from './confirm-publishing-session-modal/confirm-publishing-session-modal.component';
import {
  ConfirmSessionMoveToRecycleBinModalComponent,
} from './confirm-session-move-to-recycle-bin-modal/confirm-session-move-to-recycle-bin-modal.component';
import {
  ConfirmUnpublishingSessionModalComponent,
} from './confirm-unpublishing-session-modal/confirm-unpublishing-session-modal.component';
import {
  ResendResultsLinkToStudentModalComponent,
} from './resend-results-link-to-student-modal/resend-results-link-to-student-modal.component';
import {
  SendRemindersToStudentModalComponent,
} from './send-reminders-to-student-modal/send-reminders-to-student-modal.component';
import {
  CopySessionResult,
  SessionsTableColumn,
  SessionsTableHeaderColorScheme,
  SessionsTableRowModel, SortBy, SortOrder,
} from './sessions-table-model';

/**
 * A table to display a list of feedback sessions.
 */
@Component({
  selector: 'tm-sessions-table',
  templateUrl: './sessions-table.component.html',
  styleUrls: ['./sessions-table.component.scss'],
})
export class SessionsTableComponent implements OnInit {

  // enum
  SortBy: typeof SortBy = SortBy;
  SortOrder: typeof SortOrder = SortOrder;
  SessionsTableColumn: typeof SessionsTableColumn = SessionsTableColumn;
  FeedbackSessionSubmissionStatus: typeof FeedbackSessionSubmissionStatus = FeedbackSessionSubmissionStatus;
  FeedbackSessionPublishStatus: typeof FeedbackSessionPublishStatus = FeedbackSessionPublishStatus;
  SessionsTableHeaderColorScheme: typeof SessionsTableHeaderColorScheme = SessionsTableHeaderColorScheme;

  @Input()
  sessionsTableRowModels: SessionsTableRowModel[] = [];

  @Input()
  courseCandidates: Course[] = [];

  @Input()
  columnsToShow: SessionsTableColumn[] = [SessionsTableColumn.COURSE_ID];

  @Input()
  sessionsTableRowModelsSortBy: SortBy = SortBy.NONE;

  @Input()
  sessionsTableRowModelsSortOrder: SortOrder = SortOrder.ASC;

  @Input()
  headerColorScheme: SessionsTableHeaderColorScheme = SessionsTableHeaderColorScheme.BLUE;

  @Output()
  sortSessionsTableRowModelsEvent: EventEmitter<SortBy> = new EventEmitter();

  @Output()
  loadResponseRateEvent: EventEmitter<number> = new EventEmitter();

  @Output()
  editSessionEvent: EventEmitter<number> = new EventEmitter();

  @Output()
  moveSessionToRecycleBinEvent: EventEmitter<number> = new EventEmitter();

  @Output()
  copySessionEvent: EventEmitter<CopySessionResult> = new EventEmitter();

  @Output()
  submitSessionAsInstructorEvent: EventEmitter<number> = new EventEmitter();

  @Output()
  viewSessionResultEvent: EventEmitter<number> = new EventEmitter();

  @Output()
  publishSessionEvent: EventEmitter<number> = new EventEmitter();

  @Output()
  unpublishSessionEvent: EventEmitter<number> = new EventEmitter();

  @Output()
  sendRemindersToStudentsEvent:
    EventEmitter<{row: number, request: FeedbackSessionStudentRemindRequest}> = new EventEmitter();

  @Output()
  resendResultsLinkToStudentsEvent:
    EventEmitter<{row: number, request: FeedbackSessionStudentRemindRequest}> = new EventEmitter();

  constructor(private modalService: NgbModal) { }

  /**
   * Sorts the list of feedback session row.
   */
  sortSessionsTableRowModels(by: SortBy): void {
    this.sortSessionsTableRowModelsEvent.emit(by);
  }

  /**
   * Moves the feedback session to the recycle bin.
   */
  moveSessionToRecycleBin(rowIndex: number): void {
    this.modalService.open(ConfirmSessionMoveToRecycleBinModalComponent).result.then(() => {
      this.moveSessionToRecycleBinEvent.emit(rowIndex);
    }, () => {});
  }

  /**
   * Copies the feedback session.
   */
  copySession(rowIndex: number): void {
    const modalRef: NgbModalRef = this.modalService.open(CopySessionModalComponent);
    const model: SessionsTableRowModel = this.sessionsTableRowModels[rowIndex];
    modalRef.componentInstance.newFeedbackSessionName = model.feedbackSession.feedbackSessionName;
    modalRef.componentInstance.courseCandidates = this.courseCandidates;
    modalRef.componentInstance.sessionToCopyCourseId = model.feedbackSession.courseId;

    modalRef.result.then((result: CopySessionModalResult) => {
      this.copySessionEvent.emit({
        ...result,
        sessionToCopyRowIndex: rowIndex,
      });
    }, () => {});
  }

  /**
   * Publishes a feedback session.
   */
  publishSession(rowIndex: number): void {
    const modalRef: NgbModalRef = this.modalService.open(ConfirmPublishingSessionModalComponent);
    const model: SessionsTableRowModel = this.sessionsTableRowModels[rowIndex];
    modalRef.componentInstance.feedbackSessionName = model.feedbackSession.feedbackSessionName;

    modalRef.result.then(() => {
      this.publishSessionEvent.emit(rowIndex);
    }, () => {});
  }

  /**
   * Unpublishes a feedback session.
   */
  unpublishSession(rowIndex: number): void {
    const modalRef: NgbModalRef = this.modalService.open(ConfirmUnpublishingSessionModalComponent);
    const model: SessionsTableRowModel = this.sessionsTableRowModels[rowIndex];
    modalRef.componentInstance.feedbackSessionName = model.feedbackSession.feedbackSessionName;

    modalRef.result.then(() => {
      this.unpublishSessionEvent.emit(rowIndex);
    }, () => {});
  }

  /**
   * Resend links to students to view results.
   */
  remindResultsLinkToStudent(rowIndex: number): void {
    const modalRef: NgbModalRef = this.modalService.open(ResendResultsLinkToStudentModalComponent);
    const model: SessionsTableRowModel = this.sessionsTableRowModels[rowIndex];

    modalRef.componentInstance.courseId = model.feedbackSession.courseId;
    modalRef.componentInstance.feedbackSessionName = model.feedbackSession.feedbackSessionName;

    modalRef.result.then((remindRequest: FeedbackSessionStudentRemindRequest) => {
      this.resendResultsLinkToStudentsEvent.emit({ row: rowIndex, request: remindRequest });
    }, () => {});
  }

  /**
   * Sends e-mails to remind students who have not submitted their feedback.
   */
  sendRemindersToStudents(rowIndex: number): void {
    const modalRef: NgbModalRef = this.modalService.open(SendRemindersToStudentModalComponent);
    const model: SessionsTableRowModel = this.sessionsTableRowModels[rowIndex];
    modalRef.componentInstance.courseId = model.feedbackSession.courseId;
    modalRef.componentInstance.feedbackSessionName = model.feedbackSession.feedbackSessionName;

    modalRef.result.then((remindRequest: FeedbackSessionStudentRemindRequest) => {
      this.sendRemindersToStudentsEvent.emit({ row: rowIndex, request: remindRequest });
    }, () => {});
  }

  ngOnInit(): void {
  }

}
