package com.cs425.membership.MembershipList;

import java.util.Random;
import java.util.TreeSet;

public class MemberList {

    private TreeSet<MemberListEntry> memberList;
    private MemberListEntry creator;

    public MemberList(MemberListEntry creator) {
        assert(creator != null);

        memberList = new TreeSet<>();
        this.creator = creator;
        memberList.add(creator);
    }

    public void addEntry(MemberListEntry newEntry) {
        memberList.add(newEntry);
    }

    public void removeEntry(MemberListEntry entry) {
        memberList.remove(entry);
    }

    /**
     * For pinging neighbor
     * @return successor, or null if none exists
     */
    public MemberListEntry getSuccessor() {
        MemberListEntry successor = memberList.higher(creator);
        if (successor == null) {
            successor = memberList.first();
        }
        return successor == creator ? null : successor;
    }

    /**
     * For notifying leaves
     * @return predecessor, or null if none exists
     */
    public MemberListEntry getPredecessor() {
        MemberListEntry predecessor = memberList.lower(creator);
        if (predecessor == null) {
            predecessor = memberList.last();
        }
        return predecessor == creator ? null : predecessor;
    }

    /**
     * For gossiping
     * @return random entry not equal to the list creator
     */
    public MemberListEntry getRandomEntry() {
        int idx;
        MemberListEntry[] memberArray = memberList.toArray(new MemberListEntry[0]);
        do {
            Random generator = new Random();
            idx = generator.nextInt(memberList.size());
        } while (memberArray[idx] == creator);
        return memberArray[idx];
    }
    
}
