const sqlite3 = require('sqlite3').verbose();
const { v4: uuidv4 } = require('uuid');
const db = new sqlite3.Database('/Users/jainaluo/易思态/code/Xplay/apps/server/xplay.db');

db.serialize(() => {
  // 1. 获取所有清单
  db.all("SELECT id FROM playlists", (err, playlists) => {
    if (err) return console.error(err);
    
    // 2. 获取所有媒体
    db.all("SELECT id FROM media", (err, media) => {
      if (err) return console.error(err);
      
      console.log(`Found ${playlists.length} playlists and ${media.length} media files.`);
      
      // 3. 为每个清单添加所有媒体
      playlists.forEach(p => {
        media.forEach((m, index) => {
          const itemId = uuidv4();
          db.run("INSERT INTO playlist_items (id, \"order\", duration, playlistId, mediaId) VALUES (?, ?, ?, ?, ?)",
            [itemId, index, 15, p.id, m.id],
            (err) => {
              if (err) {
                if (err.message.includes('UNIQUE constraint failed')) {
                  // console.log('Item already exists, skipping...');
                } else {
                  console.error(err);
                }
              }
            }
          );
        });
      });
      console.log('Finished auto-populating playlists. Please restart App.');
    });
  });
});
