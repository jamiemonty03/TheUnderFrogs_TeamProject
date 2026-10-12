import { Entity, Column, PrimaryGeneratedColumn, CreateDateColumn } from 'typeorm';

@Entity('refresh_tokens')
export class RefreshToken {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'char', length: 64, unique: true })
  token_hash: string;

  @Column({ type: 'integer' })
  user_id: number;

  @Column({ type: 'uuid' })
  family_id: string;

  @Column({ type: 'timestamp' })
  expires_at: Date;

  @Column({ type: 'timestamp', nullable: true })
  revoked_at: Date | null;

  @Column({ type: 'integer', nullable: true })
  replaced_by: number | null;

  @CreateDateColumn()
  created_at: Date;
}
