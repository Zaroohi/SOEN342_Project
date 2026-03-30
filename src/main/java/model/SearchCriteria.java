package model;

import java.time.LocalDate;


public class SearchCriteria {

    private String titleKeyword;
    private TaskStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String dayOfWeek;

//---------------------------------CONSTRUCTORS---------------------------------

    public SearchCriteria() {
        this.titleKeyword = null;
        this.status = null;
        this.startDate = null;
        this.endDate = null;
        this.dayOfWeek = null;
    }

//---------------------------------GETTERS AND SETTERS---------------------------------

    public String getTitleKeyword() {
        return this.titleKeyword;
    }

    public void setTitleKeyword(String titleKeyword) {
        this.titleKeyword = titleKeyword;
    }

    public TaskStatus getStatus() {
        return this.status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return this.endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    
    public String getDayOfWeek() {
        return this.dayOfWeek;
    }

    
    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

   
    public boolean isEmpty() {
        return this.titleKeyword == null &&
               this.status == null &&
               this.startDate == null &&
               this.endDate == null &&
               this.dayOfWeek == null;
    }
}