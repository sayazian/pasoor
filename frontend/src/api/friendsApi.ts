import { request } from './http';
import type { FriendsResponse } from '../types/friend';

export function getFriends(): Promise<FriendsResponse> {
  return request<FriendsResponse>('/api/friends');
}

export function sendFriendRequest(email: string, message: string): Promise<FriendsResponse> {
  return request<FriendsResponse>('/api/friends/requests', {
    method: 'POST',
    body: JSON.stringify({ email, message })
  });
}

export function acceptFriendRequest(id: string): Promise<FriendsResponse> {
  return request<FriendsResponse>(`/api/friends/requests/${id}/accept`, {
    method: 'POST'
  });
}

export function rejectFriendRequest(id: string): Promise<FriendsResponse> {
  return request<FriendsResponse>(`/api/friends/requests/${id}/reject`, {
    method: 'POST'
  });
}
