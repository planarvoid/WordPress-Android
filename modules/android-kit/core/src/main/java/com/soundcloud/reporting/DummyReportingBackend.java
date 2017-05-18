package com.soundcloud.reporting;

public class DummyReportingBackend implements ReportingBackend {

    @Override
    public void post(Metric metric) {
        // no op
    }

}
