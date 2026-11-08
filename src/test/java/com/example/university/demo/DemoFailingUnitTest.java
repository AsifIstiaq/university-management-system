package com.example.university.demo;

import com.example.university.entity.Teacher;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DEMO ONLY - this test FAILS ON PURPOSE to show a red build in GitHub Actions.
 * It is tagged "demo-failure", which the normal build excludes (see pom.xml), so the regular
 * pipeline stays green. Run it with:  mvn test -Pdemo-failure
 */
@Tag("demo-failure")
class DemoFailingUnitTest {

    @Test
    void demo_expectedNameDoesNotMatch_failsOnPurpose() {
        Teacher teacher = new Teacher();
        teacher.setFullName("Demo Teacher");

        // Wrong expectation on purpose: the actual value is "Demo Teacher".
        assertEquals("Professor Somebody Else", teacher.getFullName(),
                "DEMO FAILURE: this unit test is meant to fail");
    }
}
