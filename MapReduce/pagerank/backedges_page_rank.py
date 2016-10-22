from simple_page_rank import SimplePageRank

"""
This class implements the pagerank algorithm with
backwards edges as described in the second part of 
the project.
"""
class BackedgesPageRank(SimplePageRank):

    """
    The implementation of __init__ and compute_pagerank should 
    still be the same as SimplePageRank.
    You are free to override them if you so desire, but the signatures
    must remain the same.
    """

    """
    This time you will be responsible for implementing the initialization
    as well. 
    Think about what additional information your data structure needs 
    compared to the old case to compute weight transfers from pressing
    the 'back' button.
    """
    @staticmethod
    def initialize_nodes(input_rdd):
        # YOUR CODE HERE
        # The pattern that this solution uses is to keep track of 
        # (node, (weight, targets, old_weight)) for each iteration.
        # When calculating the score for the next iteration, you
        # know that 10% of the score you sent out from the previous
        # iteration will get sent back.

        def emit_edges(line):
            # ignore blank lines and comments
            if len(line) == 0 or line[0] == "#":
                return []
            # get the source and target labels
            source, target = tuple(map(int, line.split()))
            # emit the edge
            edge = (source, frozenset([target]))
            # also emit "empty" edges to catch nodes that do not have any
            # other node leading into them, but we still want in our list of nodes
            self_source = (source, frozenset())
            self_target = (target, frozenset())
            return [edge, self_source, self_target]

        def reduce_edges(e1, e2):
            return e1 | e2

        def initialize_weights((source, targets)):
            old_weight = {}
            old_weight[source] = 1.0 # all backpoints are initially set back to themselves
            return (source, (1.0, targets, old_weight))

        nodes = input_rdd\
                .flatMap(emit_edges)\
                .reduceByKey(reduce_edges)\
                .map(initialize_weights)
        return nodes

    """
    You will also implement update_weights and format_output from scratch.
    You may find the distribute and collect pattern from SimplePageRank
    to be suitable, but you are free to do whatever you want as long
    as it results in the correct output.
    """
    @staticmethod
    def update_weights(nodes, num_nodes):
        # YOUR CODE HERE

        """ Mapper Phase """
        def distribute_weights((node, (weight, targets, old_weight))):
            # YOUR CODE HERE
            node_lst = []

            # staying on page
            node_weight = weight * 0.05
            
            # OPTION 3:
            node_weight += old_weight[node] * 0.10 # 10% of the old weight comes back to the node
            old_weight[node] = weight # *** WEIGHT is actually the previous weight for the next iteration ***

            node_lst.append((node, node_weight)) # add node to node_lst

            if len(targets) == 0: # case when we randomly go to a link and there are no outlinks
                for n in range(num_nodes):
                    if n == node:
                        continue
                    else:
                        w = (weight * 0.85)/(num_nodes-1)
                        node_lst.append((n, w))
            else:
                for t in targets: # distributing node's weight to its outlinks
                    target_weight = (weight*0.85)/len(targets)
                    node_lst.append((t, target_weight))

            # we use this special tuple to track the node's targets
            node_lst.append((node, targets)) # special tuple in node_lst

            # we use this to keep track of previous node's weights that contributed to the current node's weight
            node_lst.append((node, old_weight)) # another special tuple in node_lst

            return node_lst # returning a list of (node, given_weight) tuples

        """ Reducer Phase """

        def collect_weights((node, values)):
            # YOUR CODE HERE

            target_list = []
            old_weight = {}
            node_weight = 0

            for v in values:
                if isinstance(v, frozenset): # TARGETS is a frozen set
                    target_list = v
                elif isinstance(v, dict): # GIVEN_NODE_WEIGHTS is a dictionary
                    old_weight = v
                else:
                    node_weight += v

            return (node, (node_weight, target_list, old_weight)) # return the updated weight and old_weight dictionary for NODE

        return nodes\
                .flatMap(distribute_weights)\
                .groupByKey()\
                .map(collect_weights) 

    @staticmethod
    def format_output(nodes): 
        # YOUR CODE HERE
        return nodes\
                .map(lambda (node, (weight, targets, old_weight)): (weight, node))\
                .sortByKey(ascending = False)\
                .map(lambda (weight, node): (node, weight))