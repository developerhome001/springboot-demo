package com.keray.common.lock;

import java.util.concurrent.locks.ReentrantLock;

public record SingleServerLockNode(ReentrantLock lock, String key) {
}
