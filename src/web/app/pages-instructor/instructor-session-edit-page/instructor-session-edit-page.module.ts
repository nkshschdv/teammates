import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { AjaxLoadingModule } from '../../components/ajax-loading/ajax-loading.module';
import { CopySessionModalModule } from '../../components/copy-session-modal/copy-session-modal.module';
import {
  QuestionTypesSessionEditModule,
} from '../../components/question-types/question-types-session-edit/question-types-session-edit.module';
import { SessionEditFormModule } from '../../components/session-edit-form/session-edit-form.module';
import { TeammatesCommonModule } from '../../components/teammates-common/teammates-common.module';
import {
  CopyQuestionsFromOtherSessionsModalComponent,
} from './copy-questions-from-other-sessions-modal/copy-questions-from-other-sessions-modal.component';
import { InstructorSessionEditPageComponent } from './instructor-session-edit-page.component';
import { TemplateQuestionModalComponent } from './template-question-modal/template-question-modal.component';

/**
 * Module for instructor session edit page.
 */
@NgModule({
  imports: [
    AjaxLoadingModule,
    CommonModule,
    FormsModule,
    NgbModule,
    TeammatesCommonModule,
    SessionEditFormModule,
    QuestionTypesSessionEditModule,
    CopySessionModalModule,
  ],
  entryComponents: [
    TemplateQuestionModalComponent,
    CopyQuestionsFromOtherSessionsModalComponent,
  ],
  declarations: [
    InstructorSessionEditPageComponent,
    TemplateQuestionModalComponent,
    CopyQuestionsFromOtherSessionsModalComponent,
  ],
  exports: [
    InstructorSessionEditPageComponent,
  ],
})
export class InstructorSessionEditPageModule { }
