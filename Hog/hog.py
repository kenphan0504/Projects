"""The Game of Hog."""

from dice import four_sided, six_sided, make_test_dice
from ucb import main, trace, log_current_line, interact
GOAL_SCORE = 100 # The goal of Hog is to score 100 points.

######################
# Phase 1: Simulator #
######################

def roll_dice(num_rolls, dice=six_sided):
    """Roll DICE for NUM_ROLLS times. Return either the sum of the outcomes,
    or 1 if a 1 is rolled (Pig out). This calls DICE exactly NUM_ROLLS times.

    num_rolls:  The number of dice rolls that will be made; at least 1.
    dice:       A zero-argument function that returns an integer outcome.
    """
    # These assert statements ensure that num_rolls is a positive integer.
    assert type(num_rolls) == int, 'num_rolls must be an integer.'
    assert num_rolls > 0, 'Must roll at least once.'
    counter = 1
    points = 0
    pig_out = False
    while counter <= num_rolls:
        cur_die = dice()
        if cur_die == 1:
            pig_out = True
            points = 0
        points = points + cur_die
        counter += 1
    if pig_out:
        return 1
    return points

def free_bacon(opponent_score):
    """Carry out the Free Bacon rule by taking in OPPONENT_SCORE and
    return 1 more than the largest digit of OPPONENT_SCORE
    """
    opponent_score = repr(opponent_score)
    if len(opponent_score) == 1:
        return eval(opponent_score[0]) + 1
    else:
        return max(eval(opponent_score[0]), eval(opponent_score[1])) + 1

def hog_wild(score, opponent_score):
    """Carry out the Hog Wild rule by returning four-sided dice if 
    the total score of both players is a multiple of seven.
    """
    if is_mul_seven(score + opponent_score):
        return four_sided
    return six_sided

def take_turn(num_rolls, opponent_score, dice=six_sided):
    """Simulate a turn rolling NUM_ROLLS dice, which may be 0 (Free bacon).

    num_rolls:       The number of dice rolls that will be made.
    opponent_score:  The total score of the opponent.
    dice:            A function of no args that returns an integer outcome.
    """
    assert type(num_rolls) == int, 'num_rolls must be an integer.'
    assert num_rolls >= 0, 'Cannot roll a negative number of dice.'
    assert num_rolls <= 10, 'Cannot roll more than 10 dice.'
    assert opponent_score < 100, 'The game should be over.'
    if num_rolls == 0:
        return free_bacon(opponent_score)
    cur_score = roll_dice(num_rolls, dice)
    return cur_score

def select_dice(score, opponent_score):
    """Select six-sided dice unless the sum of SCORE and OPPONENT_SCORE is a
    multiple of 7, in which case select four-sided dice (Hog wild).
    """
    return hog_wild(score, opponent_score)

def is_mul_seven(n):
    """Return True if a non-negative N is a multiple of 7, otherwise
    return False.
    """
    assert type(n) == int, 'n must be an integer'
    assert n >= 0, 'n must be positive'
    if n % 7 == 0:
        return True
    return False

def is_prime(n):
    """Return True if a non-negative number N is prime, otherwise return
    False. 1 is not a prime number!
    """
    assert type(n) == int, 'n must be an integer.'
    assert n >= 0, 'n must be non-negative.'
    k = 2
    if n <= 1:
        return False
    while k < n:
        if n % k == 0:
            return False
        k += 1
    return True


def other(who):
    """Return the other player, for a player WHO numbered 0 or 1.

    >>> other(0)
    1
    >>> other(1)
    0
    """
    return 1 - who

def play(strategy0, strategy1, score0=0, score1=0, goal=GOAL_SCORE):
    """Simulate a game and return the final scores of both players, with
    Player 0's score first, and Player 1's score second.

    A strategy is a function that takes two total scores as arguments
    (the current player's score, and the opponent's score), and returns a
    number of dice that the current player will roll this turn.

    strategy0:  The strategy function for Player 0, who plays first
    strategy1:  The strategy function for Player 1, who plays second
    score0   :  The starting score for Player 0
    score1   :  The starting score for Player 1
    """

    def roll_now(score, opponent_score, strategy):
        """Help to implement play(). Returns the score of the current roll. two
        players take turn rolling until the game is over.
        """
        die_type = select_dice(score, opponent_score)
        num_dice = strategy(score, opponent_score)
        return take_turn(num_dice, opponent_score, die_type)

    who = 0  # Which player is about to take a turn, 0 (first) or 1 (second)
    while score0 < goal and score1 < goal:
        cur_score = 0
        if who == 0:
            if score1 >= goal:
                return score0, score1
            cur_score = roll_now(score0, score1, strategy0)
            score0 += cur_score
            who = other(who)

        elif who == 1:
            if score0 >= goal:
                return score0, score1
            cur_score = roll_now(score1, score0, strategy1)
            score1 += cur_score
            who = other(who)

        if score0 != score1:
            sum_score = score0 + score1
            if is_prime(sum_score):
                if score0 > score1:
                    score0 += cur_score
                    if score0 >= goal:
                       return score0, score1
                else:
                    score1 += cur_score
                    if score1 >= goal:
                        return score0, score1
    return score0, score1  

#######################
# Phase 2: Strategies #
#######################

def always_roll(n):
    """Return a strategy that always rolls N dice.

    A strategy is a function that takes two total scores as arguments
    (the current player's score, and the opponent's score), and returns a
    number of dice that the current player will roll this turn.

    >>> strategy = always_roll(5)
    >>> strategy(0, 0)
    5
    >>> strategy(99, 99)
    5
    """
    def strategy(score, opponent_score):
        return n
    return strategy

# Experiments

def make_averaged(fn, num_samples=2000):
    """Return a function that returns the average_value of FN when called.

    To implement this function, you will have to use *args syntax, a new Python
    feature introduced in this project.  See the project description.

    >>> dice = make_test_dice(3, 1, 5, 6)
    >>> averaged_dice = make_averaged(dice, 1000)
    >>> averaged_dice()
    3.75
    >>> make_averaged(roll_dice, 1000)(2, dice)
    6.0

    In this last example, two different turn scenarios are averaged.
    - In the first, the player rolls a 3 then a 1, receiving a score of 1.
    - In the other, the player rolls a 5 and 6, scoring 11.
    Thus, the average value is 6.0.
    """
    def averaged(*args):
        counter = 0
        count_value = 0
        total_value = 0
        while counter < num_samples:
            fn_value = fn(*args)
            if fn_value >= 0:
                count_value += 1
                total_value += fn_value
            counter += 1
        return total_value / count_value
    return averaged

def max_scoring_num_rolls(dice=six_sided):
    """Return the number of dice (1 to 10) that gives the highest average turn
    score by calling roll_dice with the provided DICE.  Assume that dice always
    return positive outcomes.

    >>> dice = make_test_dice(3)
    >>> max_scoring_num_rolls(dice)
    10
    """
    num_of_dice = 2
    highest_average = make_averaged(roll_dice)(1, dice)
    lowest_num_rolls = 1
    while num_of_dice <= 10:
        cur_average = make_averaged(roll_dice)(num_of_dice, dice)
        if cur_average > highest_average:
            highest_average = cur_average
            lowest_num_rolls = num_of_dice
        num_of_dice += 1
    return lowest_num_rolls

def winner(strategy0, strategy1):
    """Return 0 if strategy0 wins against strategy1, and 1 otherwise."""
    score0, score1 = play(strategy0, strategy1)
    if score0 > score1:
        return 0
    else:
        return 1

def average_win_rate(strategy, baseline=always_roll(5)):
    """Return the average win rate (0 to 1) of STRATEGY against BASELINE."""
    win_rate_as_player_0 = 1 - make_averaged(winner)(strategy, baseline)
    win_rate_as_player_1 = make_averaged(winner)(baseline, strategy)
    return (win_rate_as_player_0 + win_rate_as_player_1) / 2 # Average results

def run_experiments():
    """Run a series of strategy experiments and report results."""
    if False: # Change to False when done finding max_scoring_num_rolls
        six_sided_max = max_scoring_num_rolls(six_sided)
        print('Max scoring num rolls for six-sided dice:', six_sided_max)
        four_sided_max = max_scoring_num_rolls(four_sided)
        print('Max scoring num rolls for four-sided dice:', four_sided_max)

    if False: # Change to True to test always_roll(8)
        print('always_roll(8) win rate:', average_win_rate(always_roll(8)))

    if False: # Change to True to test bacon_strategy
        print('bacon_strategy win rate:', average_win_rate(bacon_strategy))

    if False: # Change to True to test prime_strategy
        print('prime_strategy win rate:', average_win_rate(prime_strategy))

    if True: # Change to True to test final_strategy
        print('final_strategy win rate:', average_win_rate(final_strategy))

    "*** You may add additional experiments as you wish ***"

# Strategies

def bacon_strategy(score, opponent_score, margin=8, num_rolls=5):
    """This strategy rolls 0 dice if that gives at least MARGIN points,
    and rolls NUM_ROLLS otherwise.
    """
    if free_bacon(opponent_score) >= margin:
        return 0
    return num_rolls

def prime_strategy(score, opponent_score, margin=8, num_rolls=5):
    """This strategy rolls 0 dice when it results in a beneficial boost and
    rolls NUM_ROLLS if rolling 0 dice gives the opponent a boost. It also
    rolls 0 dice if that gives at least MARGIN points and rolls NUM_ROLLS
    otherwise.
    """ 
    score += free_bacon(opponent_score)
    total_score = score + opponent_score
    if is_prime(total_score):
        if score > opponent_score: 
            return 0
        elif score < opponent_score:
            return num_rolls
    return bacon_strategy(score, opponent_score, margin, num_rolls)
    
def final_strategy(score, opponent_score):
    """ This strategy contains aspects of both prime_strategy and bacon_strategy.

    -By taking use of free bacon, it will always return 0 if it GOAL_SCORE can be
    reached with it.
    -If roll 0 leads to hogtimus_prime that can reach GOAL_SCORE then return 0.
    -When the SCORE is close to 100 (above 85) it will take smaller risk when rolling.
    It makes use of the Hog Wild rule and will roll 10 dice when OPPONENT_SCORE is 
    1 away from being a multiple of 7.
    """
    score_after_bacon = score + free_bacon(opponent_score)
    if score_after_bacon >= GOAL_SCORE: #if rolling 0 die can win then do it
        return 0
    if is_prime(score_after_bacon + opponent_score): #if rolling 0 die allows hogtimus_prime and win then do it
        if score_after_bacon > opponent_score and score_after_bacon + free_bacon(opponent_score) >= GOAL_SCORE:
            return 0
    if score >= 85:
        if score >= 90 : #roll 0 to win if opponent has 89 or above points while I have 90+ pts
            if opponent_score >= 89:
                return 0
            elif score - opponent_score > 30:#if leading and very close to winning then take smaller risk
                return 2
        return 3
    if (opponent_score + 1) % 7 == 0: # if opponent is 1 pt away from hog wild, then roll 10 dice to let him have it
        return 10
    if is_mul_seven(score + opponent_score): #if have four-side dice then play safe
        if score > opponent_score:
            return 0
        else:
            return prime_strategy(score, opponent_score)
    if opponent_score - score >= 25: #if losing by more than 25 then take bigger risk
       return 9
    elif score - opponent_score >= 25: #if winning by atleast 25 then take smaller risk
       return 3
    return 5
##########################
# Command Line Interface #
##########################

# Note: Functions in this section do not need to be changed.  They use features
#       of Python not yet covered in the course.


@main
def run(*args):
    """Read in the command-line argument and calls corresponding functions.

    This function uses Python syntax/techniques not yet covered in this course.
    """
    import argparse
    parser = argparse.ArgumentParser(description="Play Hog")
    parser.add_argument('--run_experiments', '-r', action='store_true',
                        help='Runs strategy experiments')
    args = parser.parse_args()

    if args.run_experiments:
        run_experiments()
