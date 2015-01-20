package assignment_mazeworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import assignment_mazeworld.SearchProblem.SearchNode;

public class InformedSearchProblem extends SearchProblem {

	public List<SearchNode> astarSearch() {

		resetStats();

		PriorityQueue<SearchNode> frontier = new PriorityQueue<SearchNode>();
		HashMap<SearchNode, Double> explored = new HashMap<SearchNode, Double>();

		frontier.add(startNode);
		explored.put(startNode, startNode.priority());
		
		while (!frontier.isEmpty()) {

			SearchNode blah = frontier.poll();
			
			if (explored.containsKey(blah)) {
				if (explored.get(blah) < blah.priority()) 
					continue;
				
			}
			incrementNodeCount();
			updateMemory(frontier.size() + explored.size());

			if (blah.goalTest()) {
				return backchainz(blah);
			}

			ArrayList<SearchNode> successors = blah.getSuccessors();
			for (SearchNode node : successors) {

				if (!explored.containsKey(node)){
					frontier.add(node);
					explored.put(node, node.priority());
				}
				else if(explored.containsKey(node)){
					if(explored.get(node) > node.priority()){
						explored.remove(node);
						explored.put(node, node.priority());
						frontier.add(node);
					}	
				}

			}
			
		}

		return null;
	}

	protected List<SearchNode> backchainz(SearchNode node) {

		LinkedList<SearchNode> solution = new LinkedList<SearchNode>();
		solution.addFirst(node);

		while (node.getParent() != null) {
			solution.addFirst(node.getParent());
			node = node.getParent();
		}

		return solution;
	}

}
