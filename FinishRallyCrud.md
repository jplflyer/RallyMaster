# Complete CRUD for Rallies
We need to finish implementing the rest of the CRUD for rallies.

What is working:

* Retrieve rallies, either by ID or with a search
* Rally Master can create a new Rally
* And perform updates
* And delete is written.

What is missing:

* Riders
  * Register for a rally. We should ensure the rider doesn't accidentally register twice.
  * Fill out their EarnedBonusPoints and EarnedCombinations. See About Scoring
 
* Rally Masters
  * CRUD on the list of Bonus Points
  * CRUD on the list of Combinations
    * Which include the Combination Points -- which map the Combination to the required Bonus Points.
  * Promote a Rider to an Organizer or Aide.

* Scorers (either a Rally Master or an Aide)
  * Fill out the rider's EarnedBonusPoints and EarnedCombinations.
  * Edit them in order to set the `confirmed` flag on each.

I want registration and the basic CRUD in RallyController. I want scoring features in a new Controller and Service:
ScoringController and ScoringService.

# About Scoring
Scoring can be either a 1-part or 2-part process. In the 2-part process, a rider will actually fill out electronically
his rally log. He'll list each Bonus Point he visited, with time and odometer. He'll also indicate which Combinations
he believes he completed.

However, we may also use a 1-part system. The rider may submit a paper rally log, and the scorer (either a
Rally Master or Aide) will enter the Bonus Points and claimed Combinations.

Either way, the scorer then verifies the rider's evidence. How this happens may vary, but the two I've experienced are:

* The rider answers a question about the stop. For instance, at a stop in the Dog Daze rally, I was asked for the name of a pet bear.
* The rider takes a photograph and shows the photo to the scorer.
