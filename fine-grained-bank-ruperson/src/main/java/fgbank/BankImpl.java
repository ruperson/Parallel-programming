package fgbank;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BankImpl implements Bank {

    private final Account[] accounts;

    public BankImpl(int n) {
        accounts = new Account[n];
        for (int i = 0; i < n; i++) {
            accounts[i] = new Account();
        }
    }

    @Override
    public int getNumberOfAccounts() {
        return accounts.length;
    }

    @Override
    public long getAmount(int index) {
        accounts[index].lock.lock();
        try {
            return accounts[index].amount;
        } finally {
            accounts[index].lock.unlock();
        }

    }

    @Override
    public long getTotalAmount() {
        long sum = 0;
        try {
            for (Account account : accounts) {
                account.lock.lock();
                sum += account.amount;
            }
        } finally {
            for (Account account : accounts) {
                account.lock.unlock();
            }
        }
        return sum;
    }

    public long deposit(int index, long amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount: " + amount);

        accounts[index].lock.lock();
        try {
            if (amount > MAX_AMOUNT || accounts[index].amount + amount > MAX_AMOUNT)
                throw new IllegalStateException("Overflow");
            accounts[index].amount += amount;
            return accounts[index].amount;
        }
        finally {
            accounts[index].lock.unlock();
        }
    }

    @Override
    public long withdraw(int index, long amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount: " + amount);

        accounts[index].lock.lock();
        try {
            if (accounts[index].amount - amount < 0)
                throw new IllegalStateException("Underflow");
            accounts[index].amount -= amount;
            return accounts[index].amount;
        }
        finally {
            accounts[index].lock.unlock();
        }
    }

    @Override
    public void transfer(int fromIndex, int toIndex, long amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount: " + amount);
        if (fromIndex == toIndex)
            throw new IllegalArgumentException("fromIndex == toIndex");

        if (fromIndex < toIndex) {
            accounts[fromIndex].lock.lock();
            accounts[toIndex].lock.lock();
        } else {
            accounts[toIndex].lock.lock();
            accounts[fromIndex].lock.lock();
        }


        try {
            if (amount > accounts[fromIndex].amount)
                throw new IllegalStateException("Underflow");
            else if (amount > MAX_AMOUNT || accounts[toIndex].amount + amount > MAX_AMOUNT)
                throw new IllegalStateException("Overflow");
            accounts[fromIndex].amount -= amount;
            accounts[toIndex].amount += amount;
        }
        finally {
            accounts[fromIndex].lock.unlock();
            accounts[toIndex].lock.unlock();
        }
    }

    private static class Account {
        private final Lock lock = new ReentrantLock();
        long amount;
    }
}
