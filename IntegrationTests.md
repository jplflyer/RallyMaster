# New Integration Tests needed
We create Integration Tests in RallyServer under src/test. They end in IT so we know they are Integration
Tests instead of Unit Tests (which end in Test). We currently have a very small RallyControllerIT. We need to
expand this.

Our first test just verifies all the plumbing in getting tests running.

There is more background on how to write the tests below.

# New Tests
We need a lot more tests. We want to fully test the entire set of API calls.

I want to break tests by basic group. This may not be a complete list:

* Rally Master operations
* Rider creation and rally registration
  * This should include things like a variety of seaching operations. Imagine someone is looking for rallies.
  * Can we create test rallies around the world and test things like "all rallies within 500 miles" or "all rallies in Europe"?
  * I don't mind if that search is imperfect. If it includes something that's 540 miles away but missing something 490 miles away,
    I don't care. But it shouldn't be off by a whole lot. If I say 50 miles, I don't want something halfway across the country.
* Rider scoring operations
* Scorer (Rally Master or Aide) operations

I would prefer very complete tests, which includes:

* Happy Path
* Authorization failures, especially a rider doing things he shouldn't or people seeing data they shouldn't
* Data validation issues
  * Especially pushing null data. NPEs are embarrassing and need to be detected and fixed.

I would expect many, perhaps most tests to span multiple endpoints.

Each test should be atomic. That is:

* Tests that have run before it shouldn't hurt it
* But it also shouldn't depend on them. If you need a Rally, create it in the test.

When I'm going to be doing something over and over (like creating rallies), I make helper methods. Given that so many
of the tests are going to create rallies, I'd put that helper method in IntegrationTest.java and call it from the
individual tests as needed. You might even have more than one version, or the ability to provide some information,
as we want to test the search capabilities.

# Let's Create More Users
See comments about using DataFaker. Feel free to create test users. We should have:

* Multiple riders
* Multiple organizers
* A few aides

Most operations could be done with very few users, but we need to test an organizer promoting someone into a
co-organizer or an aide.

# Clean Up Test Data
Let's add to IntegrationTest's initialize() method to clean up old test data. I like to put it in the do-once
startup code like this rather than in some sort of @AfterAll or @AfterEach so that if a test fails, I can go
look at what is in the database for clues why it failed.

We should identify all new Rallies with a name starting with AutoTest. This means we can search for them
and then readily use the Delete method on the rally. If that's written properly and/or we have proper cascades
defined, it will delete all data on the rally.

Test users can also be deleted based on having an email address ending in nowhere.com as discussed under using
DataFaker.

For the scope of the cleanup, definitely clean up the test rallies our tests are creating. If we load the
test users and don't create them if we already have enough, we don't need to delete them. The main goal is I
don't want a polluted database that grows every time I run the tests.

# Use DataFaker
Claude added datafaker for us. We can use it to generate rally names and rider names. We're not currently
doing anything to force email lookups, to validate new users, but let's give fake users fake domain names,
and if we ever do start sending email, we'll specifically exclude our fake domains. Let's do something like

first.last@faker company abbreviation.nowhere.com

If we do this, then we also know any user with nowhere.com is a fake user.

For their passwords, I added this line to .integration.properties:

test_user_password = whatever

and this to IntegrationTest.java:

    protected static String testUserPassword;

So we can use that when creating or authenticating as those users.

# New Members Needed
We should create some test user

# Background
All Integration Tests inherit from IntegrationTest. He has a BeforeEach which logs in for us and sets
headers. So we can use these methods:

```
    protected <T> T get_ForRM(String path, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.get(path, organizerAuthHeader, typeRef);
    }

    protected <T> T post_ForRM(String path, Object body, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.post(path, body, organizerAuthHeader, typeRef);
    }

    protected <T> T put_ForRM(String path, Object body, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.put(path, body, organizerAuthHeader, typeRef);
    }

    protected <T> T delete_ForRM(String path, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.delete(path, organizerAuthHeader, typeRef);
    }

    protected <T> T get_ForRider(String path, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.get(path, riderAuthHeader, typeRef);
    }

    protected <T> T post_ForRider(String path, Object body, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.post(path, body, riderAuthHeader, typeRef);
    }

    protected <T> T put_ForRider(String path, Object body, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.put(path, body, riderAuthHeader, typeRef);
    }

    protected <T> T delete_ForRider(String path, TypeReference<T> typeRef) throws IOException, InterruptedException {
        return restCaller.delete(path, riderAuthHeader, typeRef);
    }
```

The first set are when we want a RallyMaster (or organizer) to make the call. We use the second set when
we want a Rally Rider to make the call. Rally Masters own the rallies they are organizing and have more
privileges.