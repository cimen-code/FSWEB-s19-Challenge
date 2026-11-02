import React, { useState } from "react";

export default function App() {

  const [userId, setUserId] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const [tweets, setTweets] = useState([]);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  async function loadTweets(event) {
    event.preventDefault();

    setLoading(true);
    setMessage("");
    setTweets([]);

    try {
      const credentials = new TextEncoder().encode(
        `${username}:${password}`
      );

      const encoded = btoa(
        Array.from(credentials, byte =>
          String.fromCharCode(byte)
        ).join("")
      );

      const url =
        "http://localhost:3000/tweet/findByUserId" +
        `?userId=${encodeURIComponent(userId)}`;

      const response = await fetch(url, {
        method: "GET",
        headers: {
          Authorization: `Basic ${encoded}`
        }
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();

      if (!Array.isArray(data)) {
        throw new Error("Unexpected API response");
      }

      setTweets(data);

      if (data.length === 0) {
        setMessage("Bu kullanıcıya ait tweet bulunamadı.");
      }

    } catch (error) {
      setMessage(`İstek başarısız: ${error.message}`);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main style={{
      maxWidth: 600,
      margin: "40px auto",
      fontFamily: "Arial, sans-serif",
      padding: 16
    }}>
      <h1>Twitter API</h1>
      <p>Workintech Sprint 19 Challenge</p>

      <form onSubmit={loadTweets}>
        <p>
          <label>
            Kullanıcı adı
            <input
              type="text"
              value={username}
              onChange={e => setUsername(e.target.value)}
              required
              style={{ display: "block", width: "100%" }}
            />
          </label>
        </p>

        <p>
          <label>
            Şifre
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              style={{ display: "block", width: "100%" }}
            />
          </label>
        </p>

        <p>
          <label>
            Tweetleri getirilecek kullanıcı ID
            <input
              type="number"
              min="1"
              value={userId}
              onChange={e => setUserId(e.target.value)}
              required
              style={{ display: "block", width: "100%" }}
            />
          </label>
        </p>

        <button type="submit" disabled={loading}>
          {loading ? "Yükleniyor..." : "Tweetleri Getir"}
        </button>
      </form>

      <h2>Tweetler</h2>

      {message && <p role="status">{message}</p>}

      <ul>
        {tweets.map(tweet => (
          <li key={tweet.id}>
            <strong>{tweet.username}</strong>
            {" — "}
            {tweet.content}
          </li>
        ))}
      </ul>
    </main>
  );
}
