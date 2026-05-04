export type FriendshipStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export interface FriendSummary {
  id: string;
  name: string;
  email: string;
  avatarUrl: string | null;
}

export interface FriendRequestSummary {
  id: string;
  requester: FriendSummary;
  recipient: FriendSummary;
  status: FriendshipStatus;
  message: string | null;
}

export interface FriendsResponse {
  friends: FriendSummary[];
  incomingRequests: FriendRequestSummary[];
  outgoingRequests: FriendRequestSummary[];
}
