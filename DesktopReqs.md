# Desktop App Requirements
There will be one desktop application, written using Kotlin Compose 
Multiplatform. We'll be writing on MacOS but also want to deploy
on Windows.

The Desktop App will support Rally Masters, Scorers, and Riders.
However, we'll discuss Rider requirements in a separate document.
For now, we're focusing on the Rally Master and Scoring process.

# Data Storage
I want to make use of the REST Server entirely optional. Thus, the
Rally Master can decide whether to store a Rally locally in JSON files
or on the server. This will be discussed further.

# On Startup
When running the application, display a splash screen as soon as possible.
Unless clicked, it should remain visible for at least 5 seconds.

If the user has previously logged into the server, automatically log in.

If the user has any past rallies, display that list. Include an Open
button for each.

If the user has no past rallies listed, provide a helpful screen.

Then provide buttons for the following:

* (If not logged in) Login
* Join a Rally (if logged in)
* Create a Rally
* Import a Rally

## Import a Rally
As stated, Rally Masters can work with local files or on the server.
When working locally, they can export their rally to share it with
other Rally Masters or Scorers. This is a simple JSON file.

The Import function is the reverse. They may have downloaded a
JSON file stored in ~/Downloads (on a Mac), for instance. Importing
a Rally is just the following:

* Provide a file selector to find the JSON file
* Load and verify the contents
* Save to our standard location
* Then go to the View Rally page as if they had just opened it.

## Create a Rally
Creating a Rally will give them the option to work with the server
or storing locally. If storing locally, ask for the Rally Name, then
create the initial JSON and go to the Rally Details page. If
working with the server, just remember that and go to the Rally Details
page. Save to the serer once they've entered the initial details.

## Open a Rally
They get to this page either because they did Open on an existing Rally,
Created a Rally, or Imported a Rally.

At the top will be very brief rally information with an View Details
button. (This page will also support edits.)

We'll then have a tabbed window with bonus points and combinations.

We also need to let them import data from spreadsheets. They may be doing
planning in other tools. We'll support importing two types of CSV files:

* Bonus points
* Combinations

We'll determine more requirements when we implement this feature.


# User Preferences
We want to store user preferences. This should be stored in a native
location which will very between Mac and Windows. I believe on Mac, it
is customary to store under ~/Library/Application Support/ProductName.
I don't know where they go under Windows.

We will store:

* login information. If we can use a native, secure method for storing the
password, that is good.
* Past Rallies. See below.

## Past Rallies
Keep a history of all past rallies, including any stored locally.