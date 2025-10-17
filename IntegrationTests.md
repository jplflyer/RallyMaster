# Creating Integration Tests
So far, I've been testing manually with curl. It's time to start creating integration tests.

In the day job, we separate the concept of unit tests (which are small and easily run standalone) with
integration tests, which need to perform REST calls against a running server. To do this, we separate
them. Unit tests end with Test such as StringToolsTest.java. Integration tests end in IT for
Integration Test such as MemberControllerIT.

All Integration Tests inherit from a common IntegrationTest parent class, which knows all sorts of things.

We then use surefire to run unit tests and failsafe to run Integration tests.

# Task One: Update RallyServer's build.gradle.kts
I believe we're already configured to be able to run unit tests. I'm accustomed to using Maven at work.
I don't know what we need to do to be able to keep unit tests and integration tests separate.

Please do the magic on build.gradle.kts and also comment how to run each set of tests.