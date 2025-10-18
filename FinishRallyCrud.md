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
    * We do not need additional information at this time.
    * Riders register in advance.
  * Fill out their EarnedBonusPoints and EarnedCombinations. See About Scoring
    * See EarnedBonusPoint and EarnedCombination.
    * These are the same call that Scorers can do below.
 
* Rally Masters
  * CRUD on the list of Bonus Points.
    * See BonusPoint.java for the fields.
  * CRUD on the list of Combinations.
    * Which include the Combination Points -- which map the Combination to the required Bonus Points.
    * See Combination.java and CombinationPoint.java.
  * Promote a Rider to an Organizer or Aide.
    * Note that Rally Master and Organizer are synonyms. I should just pick one.
    * Rally Masters can edit the rally information. Other aides can assist with scoring.

* Scorers (either a Rally Master or an Aide)
  * Register rider starting odometer at the start of the rally.
    * This is stored in RallyParticipant.
  * Register rider ending odometer at the end of the rally.
    * Also stored in RallyParticipant.
  * Fill out the rider's EarnedBonusPoints and EarnedCombinations.
    * See EarnedBonusPoint and EarnedCombination.
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

We do not need to store the evidence (for now). I may add something later.