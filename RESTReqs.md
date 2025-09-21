# REST Server Requirements
This describes the requirements for the REST Server. This is a
Java Spring Boot / Data / Web application.

# Authentication
Authentication to the server is initially via Basic or Digest
username/password. This will return both a short-lived JWT and
a long-lived, use-once refresh token.

Use of the refresh token will return a new refresh token and a new
JWT.

All other calls will receive the JWT as a Bearer authentication header.

# Required Capabilities

* Register
* Login
* Refresh Token

* Fetch My Rallies
* Find a Rally: this is a search
* Join a Rally

* Full CRUD on a Rally for Rally Masters
* Full CRUD on Scoring for Rally Scorers

* For Riders: full CRUD on my completed bonus points and combinations

In CRUD, the delete methods in production will only mark a deleted status
without deleting data. These methods will take an optional (hidden) "permanently" flag that defaults to false. Only in desktop or development
mode will we actually delete from the database.

# About the Code

* Use Project Lombok.
* Use the @Slf4j annotation for logging

# About Testing
Unit tests are simple tests that can run standalone. For instance, it's
easy to unit test a string manipulation class.

Integration Tests require a running server. They test the REST calls
themselves.

We should organize code to make it as unit-testable as possible while
not excessively relying on mocking.

All REST calls should be fully integration tested.

Name unit test classes with names ending in Test. Name integration tests
with names ending in IT. This makes it easy to set up to run them
differently.