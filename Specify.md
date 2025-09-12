# Overview
Create an expansive set of applications to support motorcycle rallies.

# What is a Motorcycle Rally
As an example, we'll use a recent rally I attended, the Dog Daze Rally hosted by Team Strange. This was a one-day event centered around
Brainerd, Minnesota. The rally included 86 bonus point locations within about 50 miles of Brainerd. From there, it is a
sort of scavenger hunt where you visit the locations and accrue points. Each location is worth assigned points, from
about 10 to 75, as determined by the rally organizers. In addition, there were combinations of as few as three to
as many as about 8 bonus points. If you visited each of the required points, you could score the combination's points.
Combination -- or combo -- points were worth a low of around 200 points to a high of 1000 points. There were so many
points that you couldn't visit all 86 within a day. People were visiting from about 15 or 20 to perhaps as many as 45.
I completed 7 combinations and scored well, but the winners of the rally rode further and either completed higher point
combos than I did or just did closer to 10 instead of 7.

# Terms
* Rally: A particular event. Two examples are the Dog Daze Rally as described
earlier or the Iron Butt Rally, which is an 11-day event.
* Rally Master: anyone organizing the rally.
* Rally Rider: anyone participating as a motorcycle rider.
* Scorer: Someone helping to score the rally at the end. Rally Masters may act as scorers.
* Bonus Point: a particular stop. This term is somewhat misleading, as bonus points are worth points. We're using both definition of the word point. Note that bonus points have a name, an abbreviation, GPS coordinates, a description, possibly an address, and the number of points.
* Combo or Combination: a set of bonus points that if fully visited yields extra points.

# Rally Master Desktop
We need a desktop application for the Rally Masters to use in planning.

Data can be stored either:

* Locally
* On the server

Rally Masters may prefer to store data locally to ensure the managers of the server cannot have sneak peaks. When using the server, we should automatically
also keep backups of the data in case there is a server misfunction.

## Actions Rally Masters Need:
* Store preferences. These preferences will include encrypted login credentials as well as a history of their rallies.
* Open a rally. This can either come from their history or they can open a new file they've received from another Rally Master or can open a rally from the server.
* Create a Rally. We'll ask them if they want to store data locally or on the server. When working locally, get the file location during the create.
* Enter / Edit all basic information. This would include the rally name, description, dates.
* Enter / Edit bonus points.
* Create / Edit combinations.
* View Registered Riders.

## Nice to Have
* Can we give them a map view with all their points?

## Scoring Functions
Scorers will also use the Rally Master Desktop to perform scoring. They will load the rally data then switch to scoring mode. By this time, all Rally Riders will have both their name and a rider number.

Scorers are probably given a written log by the rider. They should be able to find the rider very easily from either the rider name or the rider number.

From this point, the scorer should be able to just enter the bonus points earned. The scoring system will determine the points for each bonus point as well as the completed combinations with their points.

Note that there will be discrepancies. A rider may feel they earned a point, but they may have made a mistake, either being at the wrong point, answering a question wrong, or in some other way making a mistake. The scoring section should accept all points reported by the rider with the ability to mark approved or not. As most points will be approved, Yes should be the default value.

When done, the scoring system should calculate the final score. When working locally, this gets stored to JSON. As there will be several scorers, we'll need to be able to merge data from multiple scorers later.

When working against the server, store the information on the server for that rider.

# REST Server
Where will be a Java Spring Boot REST server to support all other applications.
Functionality to support includes:

* Signup: This will allow someone to create a new account.
* Login: Accepts username and password and returns a JWT on success. All other calls that require an authorizes user will expect a Bearer JWT authentication header.
* Rally Master Support: Register a new (future) Rally
* Rally Master Support: Full CRUD on all rally data
* Rider Support: Find a rally and register for it
* Scorers: Save per-rider scoring information.

Additional features may be defined later.

# Web UI
The Web UI will support both Rally Masters and Rally Riders. Note that Rally Masters for one Rally may be Riders for another Rally.

Features:

* Signup
* Login
* View My Rides
* Find a Ride
* Register for a Ride

# Rally Rider Desktop
We then need a desktop app for Rally Riders. We'll need the same functionality as provided by the Web UI, but we're also going to incorporate ride planning.

Ride planning begins with receiving the rally data -- the rally points and their combinations. The Rally Master can choose how to publish this data. They might actually provide the data completely via the REST Server, but that is unlikely. There is nothing that says a Rally Master will be using our software, but the rider may still want to use us for their planning.

So data can be loaded:

* From the server
* From a a CVS or several geocoding based files such as KMZ.
* Enter manually

In my limited experience, we had simple files with all the bonus points, but the files didn't include the actual points or list the combos. So riders will need to enter some data manually, and they may choose to not enter all information if there are more points than they care to consider. For instance, for the Iron Butt Rally, points may be all the way up in Alaska. A rider may look at those and say, "No".

Either way, we need the bonus points with their abbreviations and coordinates (probably from CSV, KMZ, et cetera) as well as their point values (probably manually) as well as the combinations available, which will be easy to enter with the bonus point abbreviations as well as the total points for the combo.

From there, we want a map. This may mean Open Street Maps or some other mapping software. Display the map and display all bonus points. Color-code the points based on the various combinations. For the Dog Daze Rally, there were about 15 combinations available, so we might have 15 different colors of pins. This might get excessive, so we should have other markers. Use whatever might be standard when we later upload to Google My Maps.

They should be able to turn on or off points based on the combinations or turn off all points that aren't associated with any combos. This will help to declutter during planning.

We should be able to pick a general location and turn on/off all combinations that do/do not have points in that general area. For instance, in the Dog Daze Rally, Little Falls was necessary for several combos. If I right click near Little Falls, I could say, "Turn on (or off) all combos with points near where I clicked."

Riders can also have alternative subroutes. I did this twice on the Dog Daze rally. "If ahead of time by this location, ride out to Aitkin."

It will become an iterative process as the rider determines their overall route. We will then allow the rider to export their route in a variety of formats to be determined later, but this will include CSV, GPX, and KML/KMZ. GPX is supported natively by Garmin Basecamp used by many Rally Riders. KML is supported by Google My Maps. KMZ is a compressed version of KML. They can also save to JSON, of course.

In the CSV file, we should make sure the file can be printed cleanly without wrapping pages horizontally. We need the bonus point name and the rally book page number associated with any combinations it's part of. We want the expected departure time from the bonus point. We'll see after that.

They will also upload the data to the server for later use by the mobile app.

# Mobile Applications
Mobile implies both Android and iOS. This app is specifically for riding the planned ride.

Log in to the server and find the current ride.

Option to download the latest versions of the maps. We have to assume portions of the route will include no cell coverage.

Open the route in a map.

Assist with navigation to the next bonus point. This is very much like using Google Maps, but it should be able to run even without cell coverage, and Google Maps is very poor at multi-stop routes, especially ones as complicated as a rally.

Display "expected time to finish" at all times. Also include, "fastest time to finish". If I'm running exactly on schedule, then expected time to finish is probably a few minutes before the rally end time. (Arriving late is an automatic disqualification, after all.) Fastest time to finish is what the rider would need to know if he's running behind. If fastest time to finish begins to get too close to running late, he needs to abort and head straight to the rally finish at the best speed.

We should be able to divert to optional points when we're ahead of schedule or smartly skip stops if we're behind. Generally speaking, the rider will prefer to skip no-combo stops before combo stops, especially combo stops for which he's already made several earlier stops.

We should also be able to adjust our plan on the fly. If I'm ahead and want to make extra stops, help me pick where to go.

Can we take photos at each stop?

At any point, the rider should also be able to say, "Go to next point." Also, sometimes you have to reorder what you're doing because of poor planning or bad information. In the Dog Daze Rally, one set of GPS coordinates was off by 10 miles, and I ended up doing several points out of order before joining back to my original route.


# Languages and Frameworks
The REST server will be Java Spring Boot. All other applications will be Kotlin Compose Multiplatform. This should cover both Android and iOS for mobile and both Windows and MacOS for desktop as well as a Web UI.

The database will be PostgreSQL.

# Mapping Library Requirements
I don't know what map library we want. We have these requirements:

* Able to be used offline
* As up to date as reasonably possible
* Can do routing
* Would be convenient if we can use the same library for both desktop and mobile