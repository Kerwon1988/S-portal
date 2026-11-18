package com.simbio.core.content.api;

import java.util.List;

public interface TeamMembersReader {

    List<TeamMemberView> publicMembers(String locale);
}
