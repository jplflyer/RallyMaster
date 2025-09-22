# Authentication
RallyServer is a Spring Boot REST Server using Spring Web, Spring Data JPA,
PostgreSQL, and Spring Security.

# Member Database Table
Users will authenticate against the member table with these initial
columns.

* id: Integer. the primary key
* email: varchar. This is used as the username.
* password: varchar. This should be salted and encrypted when stored.

# Authentication Endpoint
All users will authenticate using Basic authentication. We need
a LoginController with a /login endpoint. This endpoint will return
a short-lived (duration configured in application.yml) JWT and a
long-lived (also configured in application.yml) use-once refresh token.

We will need a /token endpoint in the same controller that is used
to refresh the JWT. Return structure should match the /login call.

