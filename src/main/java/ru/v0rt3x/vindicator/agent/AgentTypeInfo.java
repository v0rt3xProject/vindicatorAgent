package ru.v0rt3x.vindicator.agent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AgentTypeInfo {
    String value();
}
