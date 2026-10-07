package com.vihan.hospital.patient;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "Similar name" for the duplicate warning (FR-PT2, DESIGN.md §7): names are compared as
 * normalised word sets, so order, titles, dots and spacing don't matter, and an initial matches a
 * word with that first letter. Examples that match: "K. Ravi Kumar" / "Ravi Kumar Kandula",
 * "Smt Lakshmi Devi" / "LAKSHMI DEVI", "Ravi" / "Ravi Kumar". Spelling variants (Laxmi / Lakshmi)
 * are not caught; the same-mobile list still shows them.
 */
public final class NameMatcher {

	private static final Set<String> TITLES = Set.of("MR", "MRS", "MS", "MISS", "DR", "SRI", "SHRI", "SMT", "KUM",
			"KUMARI", "MASTER", "BABY");

	private NameMatcher() {
	}

	static List<String> words(String name) {
		return Arrays.stream(name.toUpperCase(Locale.ROOT).replace("B/O", " ").replaceAll("[^A-Z]+", " ").strip().split(" "))
			.filter(w -> !w.isEmpty() && !TITLES.contains(w))
			.toList();
	}

	public static boolean similar(String a, String b) {
		List<String> wa = words(a);
		List<String> wb = words(b);
		Set<String> fullA = full(wa);
		Set<String> fullB = full(wb);
		if (fullA.isEmpty() || fullB.isEmpty() || fullA.stream().noneMatch(fullB::contains)) {
			return false;   // at least one whole word in common
		}
		// Every whole word of the shorter name appears in the longer one ...
		Set<String> shorter = fullA.size() <= fullB.size() ? fullA : fullB;
		Set<String> longer = shorter == fullA ? fullB : fullA;
		if (!longer.containsAll(shorter)) {
			return false;
		}
		// ... and every initial on either side stands for some word on the other.
		return initialsFit(wa, wb) && initialsFit(wb, wa);
	}

	private static Set<String> full(List<String> words) {
		return words.stream().filter(w -> w.length() > 1).collect(Collectors.toSet());
	}

	private static boolean initialsFit(List<String> from, List<String> other) {
		return from.stream().filter(w -> w.length() == 1)
			.allMatch(i -> other.stream().anyMatch(w -> w.charAt(0) == i.charAt(0)));
	}

}
