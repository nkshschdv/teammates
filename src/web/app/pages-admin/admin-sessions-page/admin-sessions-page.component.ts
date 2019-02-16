import { Component, OnInit } from '@angular/core';
import moment from 'moment-timezone';
import { HttpRequestService } from '../../../services/http-request.service';
import { StatusMessageService } from '../../../services/status-message.service';
import { TimezoneService } from '../../../services/timezone.service';
import { FeedbackSessionStats } from '../../../types/api-output';
import { ErrorMessageOutput } from '../../error-message-output';

interface OngoingSession {
  sessionStatus: string;
  instructorHomePageLink: string;
  startTime: number;
  endTime: number;
  creatorEmail: string;
  courseId: string;
  feedbackSessionName: string;
  responseRate?: string;
}

interface OngoingSessionsData {
  totalOngoingSessions: number;
  totalOpenSessions: number;
  totalClosedSessions: number;
  totalAwaitingSessions: number;
  totalInstitutes: number;
  sessions: { [key: string]: OngoingSession[] };
}

/**
 * Admin sessions page.
 */
@Component({
  selector: 'tm-admin-sessions-page',
  templateUrl: './admin-sessions-page.component.html',
  styleUrls: ['./admin-sessions-page.component.scss'],
})
export class AdminSessionsPageComponent implements OnInit {

  totalOngoingSessions: number = 0;
  totalOpenSessions: number = 0;
  totalClosedSessions: number = 0;
  totalAwaitingSessions: number = 0;
  totalInstitutes: number = 0;
  sessions: { [key: string]: OngoingSession[] } = {};

  // Tracks the whether the panel of an institute has been opened
  institutionPanelsStatus: { [key: string]: boolean } = {};

  showFilter: boolean = false;
  timezones: string[] = [];
  timezone: string = '';
  startDate: any = {};
  startTime: any = {};
  endDate: any = {};
  endTime: any = {};

  timezoneString: string = '';
  startTimeString: string = '';
  endTimeString: string = '';

  constructor(private timezoneService: TimezoneService, private httpRequestService: HttpRequestService,
      private statusMessageService: StatusMessageService) {}

  ngOnInit(): void {
    this.timezones = Object.keys(this.timezoneService.getTzOffsets());
    this.timezone = moment.tz.guess();

    const now: any = moment();
    this.startDate = {
      year: now.year(),
      month: now.month() + 1,
      day: now.date(),
    };
    this.startTime = {
      hour: now.hour(),
      minute: now.minute(),
    };
    this.endTime = {
      hour: now.hour(),
      minute: now.minute(),
    };

    const nextWeek: any = moment(now).add(1, 'weeks');
    this.endDate = {
      year: nextWeek.year(),
      month: nextWeek.month() + 1,
      day: nextWeek.date(),
    };

    this.getFeedbackSessions();
  }

  /**
   * Opens all institution panels.
   */
  openAllInstitutions(): void {
    for (const institution of Object.keys(this.institutionPanelsStatus)) {
      this.institutionPanelsStatus[institution] = true;
    }
  }

  /**
   * Closes all institution panels.
   */
  closeAllInstitutions(): void {
    for (const institution of Object.keys(this.institutionPanelsStatus)) {
      this.institutionPanelsStatus[institution] = false;
    }
  }

  /**
   * Converts milliseconds to readable date format.
   */
  showDateFromMillis(millis: number): string {
    return moment(millis).format('ddd, DD MMM YYYY, hh:mm a');
  }

  private getMomentInstant(year: number, month: number, day: number, hour: number, minute: number): any {
    const inst: any = moment.tz(this.timezone);
    inst.set('year', year);
    inst.set('month', month);
    inst.set('date', day);
    inst.set('hour', hour);
    inst.set('minute', minute);
    return inst;
  }

  /**
   * Gets the feedback sessions which have opening time satisfying the query range.
   */
  getFeedbackSessions(): void {
    const startTime: any = this.getMomentInstant(this.startDate.year, this.startDate.month - 1,
        this.startDate.day, this.startTime.hour, this.startTime.minute);
    const endTime: any = this.getMomentInstant(this.endDate.year, this.endDate.month - 1,
        this.endDate.day, this.endTime.hour, this.endTime.minute);
    const displayFormat: string = 'ddd, DD MMM YYYY, hh:mm a';
    this.startTimeString = startTime.format(displayFormat);
    this.endTimeString = endTime.format(displayFormat);
    this.timezoneString = this.timezone;

    const paramMap: { [key: string]: string } = {
      starttime: startTime.toDate().getTime(),
      endtime: endTime.toDate().getTime(),
    };
    this.httpRequestService.get('/sessions/admin', paramMap).subscribe((resp: OngoingSessionsData) => {
      this.totalOngoingSessions = resp.totalOngoingSessions;
      this.totalOpenSessions = resp.totalOpenSessions;
      this.totalClosedSessions = resp.totalClosedSessions;
      this.totalAwaitingSessions = resp.totalAwaitingSessions;
      this.totalInstitutes = resp.totalInstitutes;
      this.sessions = resp.sessions;

      this.institutionPanelsStatus = {};
      for (const institution of Object.keys(resp.sessions)) {
        this.institutionPanelsStatus[institution] = true;
      }
    }, (resp: ErrorMessageOutput) => {
      this.statusMessageService.showErrorMessage(resp.error.message);
    });
  }

  /**
   * Gets the response rate of a feedback session.
   */
  getResponseRate(institute: string, courseId: string, feedbackSessionName: string, event: any): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    const paramMap: { [key: string]: string } = {
      courseid: courseId,
      fsname: feedbackSessionName,
    };
    this.httpRequestService.get('/session/stats', paramMap).subscribe((resp: FeedbackSessionStats) => {
      const sessions: OngoingSession[] = this.sessions[institute].filter((session: OngoingSession) =>
          session.courseId === courseId && session.feedbackSessionName === feedbackSessionName,
      );
      if (sessions.length) {
        sessions[0].responseRate = `${resp.submittedTotal} / ${resp.expectedTotal}`;
      }
    }, (resp: ErrorMessageOutput) => {
      this.statusMessageService.showErrorMessage(resp.error.message);
    });
  }

}
