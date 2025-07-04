package com.db.tflm.manualcapture.action;

import io.gatling.commons.validation.Validation;
import io.gatling.core.session.Session;

@FunctionalInterface
public interface Tracker {
    Validation<Void> track(Session session);
}