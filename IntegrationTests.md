# New Integration Tests needed
We create Integration Tests in RallyServer under src/test. They end in IT so we know they are Integration
Tests instead of Unit Tests (which end in Test). We currently have a very small RallyControllerIT. We need to
expand this.

Our first test just verifies all the plumbing in getting tests running.

There is more background on how to write the tests below.

# New Tests
We need a lot more tests. We want to test full CRUD.

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