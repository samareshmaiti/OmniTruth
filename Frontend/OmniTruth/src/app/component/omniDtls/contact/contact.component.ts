import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';

interface ContactRequest {
  fullName: string;
  jobTitle: string;
  workEmail: string;
  company: string;
  topic: string;
  message: string;
}

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './contact.component.html',
  styleUrls: ['./contact.component.css']
})
export class ContactComponent {
  readonly currentYear = new Date().getFullYear();

  submitted = false;
  isSubmitting = false;

  formData: ContactRequest = this.createEmptyRequest();

  submitContact(form: NgForm): void {
    if (form.invalid) {
      form.control.markAllAsTouched();
      return;
    }

    const payload: ContactRequest = { ...this.formData };

    // TODO: Inject HttpClient and POST payload to your contact API/CRM.
    // Example: this.http.post('/api/v1/contact-requests', payload).subscribe(...)
    console.info('OmniTruth contact enquiry (prototype):', payload);

    this.submitted = true;
  }

  startNewEnquiry(): void {
    this.formData = this.createEmptyRequest();
    this.submitted = false;
  }

  private createEmptyRequest(): ContactRequest {
    return {
      fullName: '',
      jobTitle: '',
      workEmail: '',
      company: '',
      topic: '',
      message: ''
    };
  }
}
