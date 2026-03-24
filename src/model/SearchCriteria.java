package model;

import java.time.LocalDate;


public class SearchCriteria {

    private String titleKeyword;
    private TaskStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String dayOfWeek;

    //constr.
    public SearchCriteria() {
        this.titleKeyword = null;
        this.status = null;
        this.startDate = null;
        this.endDate = null;
        this.dayOfWeek = null;
    }

    //getter title keyword
    public String getTitleKeyword() {
        return this.titleKeyword;
    }

    //setter title keyword
    public void setTitleKeyword(String titleKeyword) {
        this.titleKeyword = titleKeyword;
    }

    //getter status
    public TaskStatus getStatus() {
        return this.status;
    }

    //setter status
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    //getter start date
    public LocalDate getStartDate() {
        return this.startDate;
    }

    //setter start date
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    //getter end date
    public LocalDate getEndDate() {
        return this.endDate;
    }

    //setter end date
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    //getter day of week
    public String getDayOfWeek() {
        return this.dayOfWeek;
    }

    //setter day of week
    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    //if no filters are set
    public boolean isEmpty() {
        return this.titleKeyword == null &&
               this.status == null &&
               this.startDate == null &&
               this.endDate == null &&
               this.dayOfWeek == null;
    }
}