export const formatLastSeen = (lastSeen: string | undefined, isOnline: boolean | undefined, currentTime?: Date): string => {
  if (isOnline) {
    return 'Online';
  }
  
  if (!lastSeen) {
    return 'Last seen recently';
  }
  
  const lastSeenDate = new Date(lastSeen);
  const now = currentTime || new Date();
  const diffInMs = now.getTime() - lastSeenDate.getTime();
  const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
  const diffInHours = Math.floor(diffInMinutes / 60);
  const diffInDays = Math.floor(diffInHours / 24);
  
  if (diffInMinutes < 1) {
    return 'Last seen just now';
  } else if (diffInMinutes < 60) {
    return `Last seen ${diffInMinutes} ${diffInMinutes === 1 ? 'minute' : 'minutes'} ago`;
  } else if (diffInHours < 24) {
    return `Last seen ${diffInHours} ${diffInHours === 1 ? 'hour' : 'hours'} ago`;
  } else if (diffInDays < 7) {
    return `Last seen ${diffInDays} ${diffInDays === 1 ? 'day' : 'days'} ago`;
  } else {
    // For older than a week, show the actual date
    return `Last seen ${lastSeenDate.toLocaleDateString()}`;
  }
};

export const formatMessageTime = (timestamp: string): string => {
  const date = new Date(timestamp);
  const now = new Date();
  const diffInMs = now.getTime() - date.getTime();
  const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));
  
  if (diffInDays === 0) {
    // Today - show time only
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  } else if (diffInDays === 1) {
    // Yesterday
    return 'Yesterday';
  } else if (diffInDays < 7) {
    // This week - show day name
    return date.toLocaleDateString([], { weekday: 'short' });
  } else {
    // Older - show date
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  }
};