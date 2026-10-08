import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NewInspection } from './new-inspection';

describe('NewInspection', () => {
  let component: NewInspection;
  let fixture: ComponentFixture<NewInspection>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NewInspection],
    }).compileComponents();

    fixture = TestBed.createComponent(NewInspection);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
