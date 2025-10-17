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

# Task Two: Create RESTCaller.java in RallyCommon under org.showpage.rallyserver.util.
This class will provide general REST calling services. We'll need an appropriate HTTP client.
I think at work we're using Apache's, but I'm open-minded. Let's decide which to use.

* The constructor should take the server and optional port.
* We should be able to define standard headers to always include.
  * By default, create Content-Type: application/json
* We need methods for get, put, post, and delete.
    * All methods should be generic, as we'll pass in a TypeReference for conversion.
    * Get should take 3 arguments:
      * Path relative to the server. We already specified the server, so http://localhost/foo would mean the path here is /foo.
      * The Authorization header, which can be null.
      * a TypeReferences for converting the output.
    * The other 3 calls should accept the body as an Object. Ask Jackson to convert it to JSON.

# Task Three: Move the DTOs from RallyServer to RallyCommon.
See org.showpage.rallyserver.dto.

This will be a refactor. Put them in the same package as the other dto's already in RallyCommon.

# Task Four: Create IntegrationTest.java
IntegrationTest.java's main job is to provide the common frameworks. An integration test is going to need to log in,
receiving an AuthResponse record. It will then use that as the Authorization: Bearer token for all subsequent calls.
To avoid committing passwords in the code itself, we'll read .integration.properties from the top RallyMaster directory.
See the description below.

## Step A: Create these variables:

* boolean initialized = false
* String organizerEmail
* String organizerPassword
* String riderEmail
* String riderPassord
* String organizerAuthHeader -- will be null for now
* String riderAuthHeader -- will be null for now

## Step B: Create an @BeforeEach called setup
This should call initialize().

Also create that method. It should check initialized and only do more if not set. Set the value and continue.

Read integration properties and use Java Reflection to set fields. This should populate the organizer and
rider information.

## Step C: Setup for TypeReference Definitions
All REST calls so far return Resposnse_Entity<RestResponse<Foo>>. On the client side, this means we
can use TypeReference<RestResponse<Foo>> as the type reference that gets passed to the various methods
from RestCaller.

I like to do this:

```
public static class RR_AuthResponse extends RestResponse<AuthResponse> {}

public static final TypeReference<RR_AuthResponse> tr_AuthResponse = new TypeReference<>();
```

Then in my code I can do:

```
AuthResponse rrAuth = restCaller.get("/auth/login", tr_AuthResponse);
```

## Step C: Create a convenience method check()
It should take a single argument of type RestResponse<?>. It should fail() if:

* The argument is null. Pass "null return" as the argument to fail().
* getSuccess is false. Pass getMessage() as the argument to fail().

Thus, we can do this:

FooResponse rrFoo = get_AsSM("/api/foo", tr_Foo);
check(rrFoo);

This will do both the null check and the success check.

## Step D: Create a LoginAs method
It should take a username and password. It will perform a login then create the auth header from the auth token
in the return structure. The login method returns AuthResponse, which already exists.

Back in initialize(), login as both users and store their auth headers.

## Step E: Create the REST methods
Create 8 methods, 2 each of these:

* get -- accepts path and TypeReference, which are passed to RESTCaller
* put -- also takes a body argument as the middle argument
* post -- 3 args
* delete -- 3 args

Make one called get_ForRM (rally master -- the organizer) and one called getForRider. Follow the same pattern
for the other 3 types of calls.

## Step F: Create RallyControllerIT
It should extend IntegrationTest. Set it up to use @Order annotation. I like my tests run in the order they
appear in the file.

For now, create a single test to get all rallies using /api/rallies with no arguments, and have it do
so as the rallyOrganizer using get_ForSM.

# .integration.properties
This is a very simple file with lines like `variable_name = value`. Blank lines and lines beginning with `#` are
ignored.

Use Java Reflection. Assume if you see `foo = bar` that you can call setFoo("bar"). Convert snake_case to camelCase.

We'll need to define two users with password:

organizer_email = jpl@showpage.org
organizer_password = 12345

rider_email = joe@showpage.org
rider_password = 67890
