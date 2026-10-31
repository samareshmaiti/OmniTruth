import { Component, ElementRef, HostListener, ViewChild } from '@angular/core';
import { RouterLink } from '@angular/router';

type UseCaseKey = 'logistics' | 'operations' | 'customer';
type ModalMode = 'demo' | 'signin';
type ModalState = 'form' | 'success';

interface UseCaseContent {
  kicker: string;
  title: string;
  description: string;
  question: string;
  result: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {
  readonly currentYear = new Date().getFullYear();

  isMenuOpen = false;

  // toggleMenu(): void {
  //   this.isMenuOpen = !this.isMenuOpen;
  // }
  isModalOpen = false;
  modalMode: ModalMode = 'demo';
  modalState: ModalState = 'form';
  activeUseCase: UseCaseKey = 'logistics';
  private previousBodyOverflow = '';

  readonly faqOpen: Record<string, boolean> = {
    faq1: false,
    faq2: false,
    faq3: false,
    faq4: false,
    faq5: false
  };

  readonly useCases: Record<UseCaseKey, UseCaseContent> = {
    logistics: {
      kicker: 'LOGISTICS · VERIFIED SLA',
      title: 'Know whether a shipment met its commitment.',
      description: 'Bring shipment status, delivery timestamps and driver qualifications together. OmniTruth evaluates the configured SLA rules and returns the result with source evidence.',
      question: 'Was shipment S-992 delivered within the agreed window?',
      result: 'SLA evaluation with delivery-time and source evidence.'
    },
    operations: {
      kicker: 'IT OPERATIONS · SERVICE HEALTH',
      title: 'Understand system health from source signals.',
      description: 'Connect infrastructure metrics and existing operations tools. Apply thresholds and rules consistently, then return an answer that explains which signals informed the result.',
      question: 'Is the production cluster within its operating thresholds?',
      result: 'Rule evaluation with metric values and source timestamps.'
    },
    customer: {
      kicker: 'CUSTOMER SERVICE · VERIFIED RECORDS',
      title: 'Give customers answers grounded in their own records.',
      description: 'Let an AI assistant retrieve approved order or account information while OmniTruth enforces tenant and record-level permissions before returning evidence.',
      question: 'What is the current status of my order?',
      result: 'Authorized record status with source-backed details.'
    }
  };

  @ViewChild('contactNameInput') private contactNameInput?: ElementRef<HTMLInputElement>;
  @ViewChild('signInCloseButton') private signInCloseButton?: ElementRef<HTMLButtonElement>;

  get activeUseCaseContent(): UseCaseContent {
    return this.useCases[this.activeUseCase];
  }

  toggleMenu(): void {
    this.isMenuOpen = !this.isMenuOpen;
  }

  closeMenu(): void {
    this.isMenuOpen = false;
  }

  selectUseCase(key: UseCaseKey): void {
    this.activeUseCase = key;
  }

  toggleFaq(faqId: string): void {
    if (!(faqId in this.faqOpen)) return;
    this.faqOpen[faqId] = !this.faqOpen[faqId];
  }

  isFaqOpen(faqId: string): boolean {
    return this.faqOpen[faqId] ?? false;
  }

  openModal(mode: ModalMode = 'demo'): void {
    this.modalMode = mode;
    this.modalState = 'form';
    this.previousBodyOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    this.isModalOpen = true;

    window.setTimeout(() => {
      if (mode === 'signin') {
        this.signInCloseButton?.nativeElement.focus();
      } else {
        this.contactNameInput?.nativeElement.focus();
      }
    });
  }

  closeModal(): void {
    this.isModalOpen = false;
    document.body.style.overflow = this.previousBodyOverflow;
    this.modalMode = 'demo';
    this.modalState = 'form';
  }

  onModalBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeModal();
    }
  }

  submitDemo(event: Event, form: HTMLFormElement): void {
    event.preventDefault();

    if (!form.reportValidity()) return;

    // TODO: Send the form values to your backend/CRM API.
    // Example: const payload = Object.fromEntries(new FormData(form).entries());
    this.modalState = 'success';
    form.reset();
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.isModalOpen) this.closeModal();
  }
}
