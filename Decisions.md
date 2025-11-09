# Decisions to Resolve
Let's discuss these one at a time and update this file when we have our answers.

# Rider-Specific Rally Notes
We need a concept. If I attend a Rally, there are these scenarios:

* RallyMasters are using our software, and they installed complete information, including the bonus point
  values and combination points.
* They are using our software, but they only included raw information on the bonus points without their
  values and no combination points.
* They are not using our software, and all the rider has is a CSV with the names and GPS locations plus
  a paper rally book with the remaining details.

## What does the rider do?

In the first example, the rider has all the information already available as provided by the Rally Masters,
with one key point missing. There's no estimated time spent at a stop. Some stops clearly require
more time than others, and it's evident when reading the notes.

In the third example, it's not too hard. The rider just creates a Rally and imports the data provided
electronically then can quickly add the additional details. For the Dog Daze rally, I think if we write
the UI carefully, that will only take about 15-30 minutes. That isn't at all bad.

It's the middle example that is the most difficult. The rider needs to be able to enter the bonus point
values and combinations, but he can't edit the Rally's data. But even with the first case, the rider may
want to enter an estimated stop time that varies from bonus point to bonus point given the information from
the rally book.

## Solutions

I can see a few ways to do this.

* Rider makes a private duplicate of the entire rally, then he has his own copy to update. This would take special
  care to link back to the original for scoring purposes, but it solves the issues associated with ride planning.
* For bonus points, we can create a new table with a name like Bonus Point Supplemental. It would have fields
  such as:
  * Participant ID
  * Bonus Point ID
  * value (overrides the field from Bonus Point)
  * expected stop duration
  * Notes
* When creating a Ride from a Rally, we can import all data from the Rally into the Ride. We can probably even
  use the existing Combination and Bonus Point tables, but add additional fields. So a Combination could have
  either a Rally ID or a Ride ID, plus we would add something like Original Combination ID to it, for mapping
  back to the one stored in the rally.

The second solution doesn't solve combinations if the Rally doesn't already have them stored.

In general, this is an awkward problem. Let's talk it through.

## Response to Claude's Thoughts on Using Solution 3
Claude suggested solution three with some additional comments. He suggested we import the bonus points as
waypoints and add fields to waypoint to support this. I'm comfortable with that.

What I didn't like was Claude's suggestion for handling the combinations, but that's because I'm thinking ahead
to the planning process. The rider is absolutely going to be thinking about this from the standpoint of
completing combinations -- the biggest and as many as possible. The largest combination may be a sucker combo.
It might include stops that don't synchronize well with any other combination. It may be easier to do several
medium-value combinations than the top two big ones.

So the Desktop app will need to have features to support that. Features like this:

* Map each bonus point
  * Color / shape coded by combination.
* A table with the combinations.
  * Clicking on a combination highlights its points.
  * And an option to "add these points to my final plan"
    * Maybe this is just a checkbox.
    * We can figure out the exact routing once we've determined which combos we like.
* An estimated total score
* The rider will probably add supplemental bonus points that are convenient to the general plan
  * While on the ride, these points could be skipped if behind schedule, but he absolutely does not want to skip any
    associated with a critical combination. So we need to be able to help him know which oens are skipable.

## DECISION: Use Solution 1 (Modified) - Import Rally Data into Ride

**Approved approach:** When a rider wants to plan their ride for a rally, they will select the rally and
choose "Plan My Ride". This creates a Ride and duplicates the rally's planning-relevant data (BonusPoints
and Combinations) into new rows that belong to the Ride.

### Implementation Details

**Schema changes needed:**

1. **BonusPoint table** - add fields to support either Rally OR Ride ownership:
   ```java
   @Column(name = "ride_id")
   private Integer rideId;  // Mutually exclusive with rallyId

   @Column(name = "source_bonus_point_id")
   private Integer sourceBonusPointId;  // If imported, links to original rally BonusPoint
   ```

2. **Combination table** - add fields to support either Rally OR Ride ownership:
   ```java
   @Column(name = "ride_id")
   private Integer rideId;  // Mutually exclusive with rallyId

   @Column(name = "source_combination_id")
   private Integer sourceCombinationId;  // If imported, links to original rally Combination
   ```

3. **CombinationPoint table** - add source tracking:
   ```java
   @Column(name = "source_combination_point_id")
   private Integer sourceCombinationPointId;  // If imported, links to original
   ```

4. **Waypoint table** - add planning fields:
   ```java
   @Column(name = "source_bonus_point_id")
   private Integer sourceBonusPointId;  // Links to original rally BonusPoint for scoring

   @Column(name = "estimated_stop_minutes")
   private Integer estimatedStopMinutes;  // Rider's time estimate for this stop

   @Column(name = "is_skippable")
   private Boolean isSkippable = Boolean.FALSE;  // Can skip if behind schedule
   ```

### How It Works

**Import Process:**
1. Rider registers for rally (creates RallyParticipant)
2. Rider creates "Plan My Ride" which triggers import
3. System copies BonusPoints → sets `rideId`, sets `sourceBonusPointId` to original
4. System copies Combinations → sets `rideId`, sets `sourceCombinationId` to original
5. System copies CombinationPoints → links to copied BonusPoints and Combinations, sets `sourceCombinationPointId`
6. Rider can now add/modify combinations, bonus point values, and build routes in their Ride

**Benefits:**
- ✅ Rider has full control over their planning copy
- ✅ Can add missing data (values, combinations) if rally didn't include them
- ✅ Can add custom bonus points and combinations
- ✅ Original rally data stays pristine
- ✅ Clear ownership model (check `rallyId` vs `rideId` to determine ownership)
- ✅ Desktop app can show combination planning features
- ✅ Scoring uses original rally data via source tracking fields

**Scoring:**
When rider claims a bonus point during the rally:
- EarnedBonusPoint links to the original rally BonusPoint (via `sourceBonusPointId` lookup)
- Scoring engine uses official rally data
- Rider's planning modifications don't affect scoring

### Implementation Status
**DEFERRED** - Decision documented, implementation deferred until Desktop app planning features are developed.

