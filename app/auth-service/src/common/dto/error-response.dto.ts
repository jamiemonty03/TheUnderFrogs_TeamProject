import { ApiProperty } from '@nestjs/swagger';

export class ErrorResponseDto {
  @ApiProperty({
    description: 'Machine-readable error code',
    example: 'AUTH-401',
  })
  errorCode: string;

  @ApiProperty({
    description: 'Human-readable error message',
    example: 'Invalid credentials',
  })
  message: string;
}
