import React, { useState, useEffect } from 'react'
import './index.css'

function App() {
  const [logs, setLogs] = useState([])
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [newUser, setNewUser] = useState({ name: '', email: '', balance: '' })

  const fetchData = async () => {
    try {
      const [logsRes, usersRes] = await Promise.all([
        fetch('http://localhost:8080/api/logs'),
        fetch('http://localhost:8080/api/users')
      ])
      const logsData = await logsRes.json()
      const usersData = await usersRes.json()
      setLogs(logsData.reverse())
      setUsers(usersData)
      setLoading(false)
    } catch (err) {
      console.error("Error fetching data:", err)
    }
  }

  useEffect(() => {
    fetchData()
    const interval = setInterval(fetchData, 3000)
    return () => clearInterval(interval)
  }, [])

  const simulateAttack = async () => {
    setLoading(true)
    try {
      await fetch(`http://localhost:8080/api/simulate-attack?userId=1&newValue=${Math.floor(Math.random() * 5000)}`, {
        method: 'POST'
      })
      fetchData()
    } catch (err) {
      console.error("Simulation failed:", err)
    }
  }

  const handleAddUser = async (e) => {
    e.preventDefault()
    if (!newUser.name || !newUser.email) return
    
    try {
      await fetch('http://localhost:8080/api/users', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...newUser,
          balance: newUser.balance ? parseFloat(newUser.balance) : 0
        })
      })
      setNewUser({ name: '', email: '', balance: '' })
      fetchData()
    } catch (err) {
      console.error("Failed to add user:", err)
    }
  }

  const pendingCount = logs.filter(l => l.status === 'PENDING').length

  return (
    <div className="dashboard">
      <header>
        <div className="logo">
          <h1><span className="pulse"></span> IDS SHIELD</h1>
        </div>
        <div className="actions">
          <button className="btn btn-simulate" onClick={simulateAttack}>
            Simulate DB Hijack
          </button>
        </div>
      </header>

      <div className="stats-grid">
        <div className="stat-card">
          <h3>Total Users</h3>
          <div className="value">{users.length}</div>
        </div>
        <div className="stat-card">
          <h3>Pending Alerts</h3>
          <div className="value" style={{color: pendingCount > 0 ? 'var(--warning)' : 'inherit'}}>
            {pendingCount}
          </div>
        </div>
        <div className="stat-card">
          <h3>System Status</h3>
          <div className="value" style={{color: 'var(--success)'}}>ACTIVE</div>
        </div>
      </div>

      <div className="panel">
        <div className="panel-header">
          <h2>Insert New Record</h2>
        </div>
        <form className="form-panel" onSubmit={handleAddUser}>
          <div className="form-group">
            <label>Name</label>
            <input 
              type="text" 
              placeholder="e.g. John Doe" 
              value={newUser.name}
              onChange={(e) => setNewUser({...newUser, name: e.target.value})}
            />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input 
              type="email" 
              placeholder="john@example.com" 
              value={newUser.email}
              onChange={(e) => setNewUser({...newUser, email: e.target.value})}
            />
          </div>
          <div className="form-group">
            <label>Initial Balance ($)</label>
            <input 
              type="number" 
              placeholder="1000" 
              value={newUser.balance}
              onChange={(e) => setNewUser({...newUser, balance: e.target.value})}
            />
          </div>
          <button type="submit" className="btn btn-primary" style={{height: '42px'}}>
            Add Member
          </button>
        </form>
      </div>

      <div className="panel">
        <div className="panel-header">
          <h2>Database Change Logs (Audit)</h2>
        </div>
        <table>
          <thead>
            <tr>
              <th>Timestamp</th>
              <th>User</th>
              <th>Field</th>
              <th>Old Value</th>
              <th>New Value</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {logs.map(log => (
              <tr key={log.id}>
                <td>{new Date(log.detectedAt).toLocaleString()}</td>
                <td>{log.user.name}</td>
                <td>{log.fieldAffected}</td>
                <td style={{color: 'var(--text-muted)'}}>{log.oldValue || 'N/A'}</td>
                <td>{log.newValue}</td>
                <td>
                  <span className={`status-badge status-${log.status}`}>
                    {log.status}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="panel">
        <div className="panel-header">
          <h2>Current User Data</h2>
        </div>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Email</th>
              <th>Balance</th>
            </tr>
          </thead>
          <tbody>
            {users.map(user => (
              <tr key={user.id}>
                <td>{user.id}</td>
                <td>{user.name}</td>
                <td>{user.email}</td>
                <td style={{fontWeight: 'bold'}}>${user.balance}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default App
