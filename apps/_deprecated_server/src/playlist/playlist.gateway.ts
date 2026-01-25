import {
  WebSocketGateway,
  WebSocketServer,
  OnGatewayConnection,
  OnGatewayDisconnect,
  SubscribeMessage,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { Logger } from '@nestjs/common';

@WebSocketGateway({
  cors: {
    origin: '*',
  },
  namespace: '/playlist',
})
export class PlaylistGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  private readonly logger = new Logger(PlaylistGateway.name);
  private connectedClients: Map<string, Socket> = new Map();

  handleConnection(client: Socket) {
    this.connectedClients.set(client.id, client);
    this.logger.log(`Client connected: ${client.id}`);
  }

  handleDisconnect(client: Socket) {
    this.connectedClients.delete(client.id);
    this.logger.log(`Client disconnected: ${client.id}`);
  }

  /**
   * 通知所有已连接的客户端播放列表已更新
   * @param playlistId 更新的播放列表ID
   * @param action 操作类型: 'created' | 'updated' | 'deleted'
   */
  notifyPlaylistChange(playlistId: string, action: 'created' | 'updated' | 'deleted', playlist?: any) {
    this.logger.log(`Broadcasting playlist ${action}: ${playlistId} to ${this.connectedClients.size} clients`);
    
    this.server.emit('playlist:change', {
      playlistId,
      action,
      playlist,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * 客户端可以订阅特定播放列表的更新
   */
  @SubscribeMessage('subscribe:playlist')
  handleSubscribe(client: Socket, playlistId: string) {
    client.join(`playlist:${playlistId}`);
    this.logger.log(`Client ${client.id} subscribed to playlist ${playlistId}`);
    return { success: true };
  }

  /**
   * 客户端取消订阅
   */
  @SubscribeMessage('unsubscribe:playlist')
  handleUnsubscribe(client: Socket, playlistId: string) {
    client.leave(`playlist:${playlistId}`);
    this.logger.log(`Client ${client.id} unsubscribed from playlist ${playlistId}`);
    return { success: true };
  }

  /**
   * 获取当前连接数
   */
  getConnectedClientsCount(): number {
    return this.connectedClients.size;
  }
}
