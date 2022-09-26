package com.cs425.membership.MembershipList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * Group membership list.
 */
public class MemberList implements Iterable<MemberListEntry>, Serializable {

    // TreeSet has lower insertion and deletion time
    private TreeSet<MemberListEntry> memberList;
    // self details
    private MemberListEntry owner;

    public MemberList(MemberListEntry owner) {
        assert(owner != null);

        memberList = new TreeSet<>();
        this.owner = owner;
        memberList.add(owner);
    }

    public int size() {
        return memberList.size();
    }

    public boolean addEntry(MemberListEntry newEntry) {
        return memberList.add(newEntry);
    }

    public void addNewOwner(MemberListEntry newOwner) {
        memberList.add(newOwner);
        assert(memberList.contains(newOwner));

        owner = newOwner;
    }

    public boolean removeEntry(MemberListEntry entry) {
        return memberList.remove(entry);
    }

    public boolean hasSuccessor() {
        return memberList.size() > 1;
    }

    public boolean hasPredecessor() {
        return memberList.size() > 1;
    }

    /**
     * For pinging neighbors
     * @return up to n successors, if they exist
     */
    public List<MemberListEntry> getSuccessors(int n) {
        List<MemberListEntry> successors = new ArrayList<>();

        MemberListEntry successor = getSuccessor(owner);
        
        for (int i = 0; i < n && successor != null; i++) {
            successors.add(successor);
            successor = getSuccessor(successor);
        }

        assert(successors.size() <= n);
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
        String stringMemberList = "Hostname\tPort\tTimestamp (Join)\n";
        stringMemberList += "_________________________";

        for (MemberListEntry entry: memberList) {
            stringMemberList += "\n";
            if (entry.equals(owner)) {
                stringMemberList += "Self: ";
            }
            stringMemberList += entry.toString();
        }

        return stringMemberList;
    }

    @Override
    public Iterator<MemberListEntry> iterator() {
        return memberList.iterator();
    }
    
}
