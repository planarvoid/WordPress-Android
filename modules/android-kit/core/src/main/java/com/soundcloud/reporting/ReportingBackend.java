package com.soundcloud.reporting;

public interface ReportingBackend {

    void post(Metric metric);

}
