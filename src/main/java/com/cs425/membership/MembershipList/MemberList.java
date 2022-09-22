package com.cs425.membership.MembershipList;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class MemberList {

    private TreeSet<MemberListEntry> memberList;
    private MemberListEntry owner;

    public MemberList(MemberListEntry owner) {
        assert(owner != null);

        memberList = new TreeSet<>();
        this.owner = owner;
        memberList.add(owner);
    }

    public void addEntry(MemberListEntry newEntry) {
        memberList.add(newEntry);
    }

    public void removeEntry(MemberListEntry entry) {
        memberList.remove(entry);
    }

    public boolean hasSuccessor() {
        return memberList.size() > 1;
    }

    public boolean hasPredecessor() {
        return memberList.size() > 1;
    }

    /**
     * For pinging neighbor
     * @return up to 3 successors, if they exist
     */
    public List<MemberListEntry> getSuccessors() {
        List<MemberListEntry> successors = new ArrayList<>();

        MemberListEntry successor = getSuccessor(owner);
        
        for (int i = 0; i < 3 && successor != null; i++) {
            successors.add(successor);
            successor = getSuccessor(successor);
        }

        assert(successors.size() <= 3);
        return successors;
    }

    /**
     * Gets successor of entry such that the successor isn't the member list owner
     * @param entry the entry to find the successor of
     * @return entry's successor
     */
    private MemberListEntry getSuccessor(MemberListEntry entry) {
        MemberListEntry successor = memberList.higher(entry);
        if (successor == null) {
            successor = memberList.first();
        }
        return successor == entry || successor == owner ? null : successor;
    }

    /**
     * @return predecessor, or null if none exist
     */
    public MemberListEntry getPredecessor() {
        MemberListEntry predecessor = memberList.lower(owner);
        if (predecessor == null) {
            predecessor = memberList.last();
        }
        return predecessor == owner ? null : predecessor;
    }

    @Override
    public String toString() {
        MemberListEntry[] memberArray = memberList.toArray(new MemberListEntry[0]);
        assert(memberArray.length > 0);

        String stringMemberList = memberArray[0].toString();
        for (int i = 1; i < memberArray.length; i++) {
            stringMemberList += "\n" + memberArray[i].toString();
        }

        return stringMemberList;
    }
    
}
