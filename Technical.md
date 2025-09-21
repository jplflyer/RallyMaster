# Technical Decisions
Please see Overview.md. This document describes some of the technical
decisions of the Rally Master software suite.

# IDE
The IDE of choice is IntelliJ IDEA.

# The Tools

* Rally Master Desktop App: written in Kotlin with multiplatform compose
* REST Server: written in Java Spring Boot with Spring Data and Spring Web
* Server Database: PostgreSQL
* Local files: JSON

# REST Call Security
Communications with the REST server will have a login method that uses
either Basic or Digest authentication. It will return a JWT plus a refresh
token. JWTs will be short-lived (perhaps 15 minutes). Refresh Tokens
will be longer lived, but use-once. A call to refresh will return
both a new JWT and new refresh token, and the old token will no longer
be valid.

If necessary, clients can do a new login.

# Structure and Code Reuse
As Kotlin can use Java classes, and Java can use Kotlin classes, as much
code as is reasonably should be stored in a common area.

# Spring Data Entities
The server will be Spring Boot with Spring Data talking to a PostgreSQL
database. As such, it will have classes annotated with @Entity.

All foreign key relationships should expose both the foreign key itself
as well as the relationship. For instance, imagine the relationships
between a Rally and the Participants. Tables are such:

* Member: any registered user
* Rally: a rally
* Participant: maps Members to a Rally

Thus, there is a 1-to-n relationship from Rally to Participants and a
1-to-n relationship from a Member to Participants. The Participant table
will have these columns:

* id: Primary Key
* rally_id: FK to Rally
* member_id: FK to Member
* role: an enum of possible roles (TBD)

The Java entity will have all of those fields plus:

* Rally rally;
* Member member;

Please mark these extra fields as JsonIgnore, however. When storing to
JSON, we'll just store rally_id and member_id.

# Expected Database Tables

## Rally
This represents a single rally. Columns include:

* id
* name
* description
* start_date
* end_date

The description field should allow for formatting. I don't know if this
should be Markdown or HTML. We'll need to display it, formatted, in both
the Desktop app and the eventual Web UI.

## Member
Represents one user of the system.

* id
* username
* password: store salted & encrypted
* name_first
* name_last
* units: See distance_units, a defined type
* member_since: timestamp
* membership_type: Not in Version 1
* spotwallaId: a string

## Participant

* id
* rally_id
* member_id
* role: see Rally Role, a defined type
* rider_number
* bike_make
* bike_model
* bike_range
* odometer_start
* odoemter_end
* did_not_start: a boolean
* did_not_finish: a boolean
* score
* efficiency: score / total distance

## Visited Bonus Point
This represents one rider's record of visiting a particular Bonus Point.

* id
* participant_id
* bonus_point_id
* odometer
* time: timestamp

## Completed Combination
This represents one rider's record of completing a Combination.

* id
* participant_id
* combination_id


## Bonus Point

* id
* rally_id
* code: a (usually short) alphanumeric code
* name
* description
* value: number of points
* repeatable

## Combination
* id
* rally_id
* code
* description
* value: number of points
* required_bonus_points: a number. See Combination Types for more info.

## Combination Point
This is one Bonus Point that is part of a combination. While the Dog Daze
Rally didn't do this, it's possible that a Rally Master will use a
particular Bonus Point in more than one Combination.

* id
* combination_id
* bonus_point_id
* required: a boolean

# Expected Database Types
PostgresSQL allows you to define custom types. This can be used just like
a Java Enum. We'll want:

* distance_units: Kilometers or Miles
* rally_role: see below

## Rally Role
These are the roles a person can have in the rally.

* Rally Master
* Assistant
* Rider

We'll also allow combined combinations:

* Rally Master and Rider
* Assistant and Rider