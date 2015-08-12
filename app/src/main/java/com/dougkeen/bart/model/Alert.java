package com.dougkeen.bart.model;

import java.util.ArrayList;
import java.util.List;

public class Alert {
    private String id;
    private String type;
    private String description;
    private String postedTime;
    private String expiresTime;

    public Alert(String id) {
        super();
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPostedTime() {
        return postedTime;
    }

    public void setPostedTime(String postedTime) {
        this.postedTime = postedTime;
    }

    public String getExpiresTime() {
        return expiresTime;
    }

    public void setExpiresTime(String expiresTime) {
        this.expiresTime = expiresTime;
    }

    public static class AlertList {
        private List<Alert> alerts;
        private boolean noDelaysReported;

        public List<Alert> getAlerts() {
            if (alerts == null) {
                alerts = new ArrayList<Alert>();
            }
            return alerts;
        }

        public void addAlert(Alert alert) {
            getAlerts().add(alert);
        }

        public void clear() {
            getAlerts().clear();
        }

        public boolean hasAlerts() {
            return !getAlerts().isEmpty();
        }

        public boolean areNoDelaysReported() {
            return noDelaysReported;
        }

        public void setNoDelaysReported(boolean noDelaysReported) {
            this.noDelaysReported = noDelaysReported;
        }
    }
}
